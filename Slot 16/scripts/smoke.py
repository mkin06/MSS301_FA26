"""Run both exam suites against SQL Server and their real HTTP/Feign gateways.

Requires existing schemas and seed data (database/*.sql), packaged JARs, and
DB_USERNAME/DB_PASSWORD if different from the exam defaults. No schema DDL is
performed here. Only test records with a unique prefix are inserted/updated.
"""
import argparse
import copy
import datetime
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
RESULTS = []
PREFIX = 't' + uuid.uuid4().hex[:9]


def request(base, method, path, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method,
        headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as ex:
        response = ex
    raw = response.read()
    try:
        value = json.loads(raw) if raw else None
    except (ValueError, UnicodeError):
        value = raw.decode(errors='replace')
    return response.status, value, response.headers


def check(base, label, method, path, body=None, http=200, status=1, validate=None):
    code, value, headers = request(base, method, path, body)
    assert code == http, (label, code, http, value)
    if status is not None:
        assert value['status'] == status, (label, value)
        assert re.fullmatch(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z', value['timestamp']), value
        assert 'application/json' in headers['Content-Type']
        if status != 1:
            assert value['data'] is None, value
    if validate:
        validate(value)
    RESULTS.append({'suite': base, 'scenario': label, 'method': method, 'path': path,
        'http': code, 'status': value.get('status') if isinstance(value, dict) else None, 'passed': True})
    print('PASS', label, code, flush=True)
    return value


def equal(actual, expected):
    assert actual == expected, (actual, expected)


def run_suite(suite, master, booking, gateway, credentials):
    media = master == 'User'
    mp, bp = ('users', 'posts') if media else ('rooms', 'reservations')
    mid, bid = ('userId', 'postId') if media else ('roomId', 'reservationId')
    available, unavailable = ('ACTIVE', 'SUSPENDED') if media else ('AVAILABLE', 'MAINTENANCE')
    deleted, initial = ('DEACTIVATED', 'SCHEDULED') if media else ('MAINTENANCE', 'CONFIRMED')
    key, rate, limit, level, kind = ('username', 'ratePerPost', 'maxDailyPosts', 'level', 'role') if media else ('roomNumber', 'pricePerNight', 'capacity', 'floor', 'roomType')
    start, end, count, total, prefix = ('startDate', 'endDate', 'numberOfPosts', 'totalCost', 'advertiser') if media else ('checkInDate', 'checkOutDate', 'numberOfGuests', 'totalAmount', 'guest')
    base = 'http://localhost:18080'
    processes, streams = [], []
    logdir = ROOT / '.work' / ('smoke-' + suite)
    logdir.mkdir(parents=True, exist_ok=True)
    for port in [18080,18081,18082]:
        with socket.socket() as sock:
            if sock.connect_ex(('127.0.0.1', port)) == 0:
                raise RuntimeError(f'Port {port} is already in use; do not stop unrelated services')
    try:
        for name, artifact, port in [(master+'Service',master.lower(),18081),(booking+'Service',booking.lower(),18082),(gateway,gateway.lower(),18080)]:
            folder = ROOT / suite / 'Code_PE' / ('DE190468'+name)
            jar = folder / 'target' / (artifact+'-1.0.0.jar')
            stream = (logdir / (artifact+'.log')).open('w',encoding='utf-8')
            streams.append(stream)
            env = dict(os.environ, **credentials, SERVER_PORT=str(port),
                PARENT_SERVICE_URL='http://localhost:18081',
                FIRST_SERVICE_URL='http://localhost:18081', SECOND_SERVICE_URL='http://localhost:18082')
            process = subprocess.Popen(['java','-Xmx256m','-jar',str(jar)], cwd=folder, env=env,
                stdout=stream, stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
            processes.append(process)
        for port in [18081,18082,18080]:
            deadline = time.time()+100
            while time.time()<deadline:
                for proc in processes:
                    if proc.poll() is not None:
                        raise RuntimeError(f'Service exited; inspect {logdir}')
                try:
                    code,_,_=request('http://localhost:'+str(port),'GET','/v3/api-docs')
                    if code==200: break
                except (OSError,urllib.error.URLError): pass
                time.sleep(0.7)
            else:raise RuntimeError(f'Service {port} startup timed out; inspect {logdir}')
        check(base,suite+' gateway OpenAPI','GET','/v3/api-docs',status=None)
        check(base,suite+' master OpenAPI','GET','/openapi/'+mp,status=None,
            validate=lambda v:equal(len(v['paths']),2))
        check(base,suite+' booking OpenAPI','GET','/openapi/'+bp,status=None,
            validate=lambda v:equal(len(v['paths']),2))
        for port in [18080,18081,18082]:
            code,body,headers=request('http://localhost:'+str(port),'GET','/swagger-ui.html')
            assert code==200 and 'swagger' in body.lower(), (port,code)
            RESULTS.append({'scenario':suite+' swagger '+str(port),'passed':True})
        for port,path in [(18080,'/api/'+mp),(18081,'/v3/api-docs'),(18082,'/v3/api-docs')]:
            code,_,headers=request('http://localhost:'+str(port),'OPTIONS',path,headers={
                'Origin':'https://example.test','Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'Content-Type'})
            assert code==200 and headers.get('Access-Control-Allow-Origin') in ['*','https://example.test'], (code,dict(headers))
            RESULTS.append({'scenario':suite+' CORS preflight '+str(port),'passed':True})
        code,_,headers=request(base,'GET','/api/'+mp,headers={'Origin':'https://example.test'})
        assert code==200 and len(headers.get_all('Access-Control-Allow-Origin'))==1, dict(headers)
        RESULTS.append({'scenario':suite+' gateway CORS response has no duplicates','passed':True})

        m={key:PREFIX,rate:500000,limit:3,level:1,kind:'CREATOR' if media else 'DOUBLE'}
        if media:m['email']='creator@example.com'
        result=check(base,suite+' create master defaults','POST','/api/'+mp,m,http=201,
            validate=lambda v:equal(v['data']['status'],available))
        master_id=result['data'][mid]
        detail='/api/'+mp+'/'+str(master_id)
        check(base,suite+' duplicate master','POST','/api/'+mp,m,http=226,status=3)
        check(base,suite+' master detail','GET',detail)
        second=copy.deepcopy(m);second[key]+='_b'
        second_id=check(base,suite+' second master','POST','/api/'+mp,second,http=201)['data'][mid]
        check(base,suite+' duplicate update','PUT',detail,{key:second[key]},http=226,status=3)
        check(base,suite+' update same unique key','PUT',detail,{key:m[key]})
        check(base,suite+' partial master','PUT',detail,{level:2},validate=lambda v:(equal(v['data'][key],m[key]),equal(v['data'][level],2)))
        for field,value in [(key,''),(key,'   '),(key,'x'*(31 if media else 21)),(rate,0),(rate,-1),(rate,1.123),(rate,10000000000000000),(limit,0),(limit,11),(level,0),(kind,'INVALID'),('status','INVALID')]+([('email','bad'),('email','a'*101)] if media else []):
            body=dict(m);body[field]=value
            check(base,suite+' invalid '+field+' '+str(value)[:24],'POST','/api/'+mp,body,http=406,status=2)
        check(base,suite+' missing mandatory master','POST','/api/'+mp,{},http=406,status=2)
        fractional=dict(m);fractional[limit]=2.5
        check(base,suite+' reject fractional integer master','POST','/api/'+mp,fractional,http=406,status=2)
        check(base,suite+' invalid partial blank','PUT',detail,{key:' '},http=406,status=2)
        for path in ['/api/'+mp+'?page=-1','/api/'+mp+'?size=0','/api/'+mp+'?size=101','/api/'+mp+'?page=abc','/api/'+mp+'?'+kind+'=INVALID','/api/'+mp+'?status=INVALID']:
            check(base,suite+' invalid list '+path,'GET',path,http=406,status=2)
        check(base,suite+' filtered paginated master','GET','/api/'+mp+'?'+kind+'='+m[kind]+'&status='+available+'&size=1&page=0',
            validate=lambda v:(equal(v['data']['size'],1),equal(v['data']['page'],0),equal(v['data']['first'],True)))
        for method,body in [('GET',None),('PUT',{}),('DELETE',None)]:
            check(base,suite+' missing master '+method,method,'/api/'+mp+'/2147483647',body,http=404,status=4)

        b={prefix+'Name':PREFIX+' Shop',prefix+'Email':'ads@example.com',prefix+'Phone':'0900000000',start:'25/07/2026',end:'28/07/2026',count:2,mid:master_id,total:1,'status':'CANCELLED'}
        result=check(base,suite+' create booking via Feign','POST','/api/'+bp,b,http=201,
            validate=lambda v:(equal(v['data'][total],1500000),equal(v['data']['status'],initial)))
        booking_id=result['data'][bid];bookpath='/api/'+bp+'/'+str(booking_id)
        check(base,suite+' booking detail embeds remote','GET',bookpath,
            validate=lambda v:(equal(v['data'][master.lower()][mid],master_id),equal(mid in v['data'],False),equal(v['data'][start],'25/07/2026')))
        for field,value in [(prefix+'Name',''),(prefix+'Email','bad'),(prefix+'Phone',' '),(prefix+'Phone','0'*21),(count,0),(count,4),(start,'2026-07-25'),(start,'31/02/2026'),(end,'25/07/2026'),(end,'24/07/2026'),('status','INVALID')]:
            body=dict(b);body[field]=value
            check(base,suite+' invalid booking '+field+' '+str(value), 'POST','/api/'+bp,body,http=406,status=2)
        check(base,suite+' missing mandatory booking','POST','/api/'+bp,{},http=406,status=2)
        fractional=dict(b);fractional[count]=1.5
        check(base,suite+' reject fractional integer booking','POST','/api/'+bp,fractional,http=406,status=2)
        missing=dict(b);missing[mid]=2147483647
        check(base,suite+' Feign parent not found','POST','/api/'+bp,missing,http=404,status=4)
        check(base,suite+' set parent unavailable','PUT','/api/'+mp+'/'+str(second_id),{'status':unavailable})
        blocked=dict(b);blocked[mid]=second_id
        check(base,suite+' nonavailable parent','POST','/api/'+bp,blocked,http=400,status=5)
        check(base,suite+' invalid partial dates','PUT',bookpath,{end:'24/07/2026'},http=406,status=2)
        check(base,suite+' invalid partial count','PUT',bookpath,{count:4},http=406,status=2)
        check(base,suite+' invalid update does not change SQL row','GET',bookpath,
            validate=lambda v:(equal(v['data'][end],'28/07/2026'),equal(v['data'][count],2),equal(v['data'][total],1500000)))
        check(base,suite+' update current parent rate','PUT',detail,{rate:600000})
        check(base,suite+' partial booking uses current rate','PUT',bookpath,{prefix+'Name':PREFIX+' Edited'},
            validate=lambda v:(equal(v['data'][total],1800000),equal(v['data'][end],'28/07/2026')))
        check(base,suite+' partial booking new effective dates','PUT',bookpath,{end:'27/07/2026'},validate=lambda v:equal(v['data'][total],1200000))
        check(base,suite+' activate second parent','PUT','/api/'+mp+'/'+str(second_id),{'status':available})
        check(base,suite+' update booking parent','PUT',bookpath,{mid:second_id},validate=lambda v:equal(v['data'][mid],second_id))
        check(base,suite+' filtered booking page','GET','/api/'+bp+'?'+prefix+'Name='+PREFIX+'&status='+initial+'&size=100',
            validate=lambda v:(equal(v['data']['totalElements'],1),equal(v['data']['content'][0][master.lower()][mid],second_id)))
        for query in ['size=101','page=-1','status=INVALID']:
            check(base,suite+' invalid booking list '+query,'GET','/api/'+bp+'?'+query,http=406,status=2)
        for method,body in [('GET',None),('PUT',{}),('DELETE',None)]:
            check(base,suite+' missing booking '+method,method,'/api/'+bp+'/2147483647',body,http=404,status=4)
        check(base,suite+' cancel soft delete','DELETE',bookpath,validate=lambda v:equal(v['data'],None))
        check(base,suite+' cancelled row retained','GET',bookpath,validate=lambda v:equal(v['data']['status'],'CANCELLED'))
        check(base,suite+' deactivate master','DELETE',detail,validate=lambda v:equal(v['data'],None))
        check(base,suite+' master soft deleted row retained','GET',detail,validate=lambda v:equal(v['data']['status'],deleted))
        check(base,suite+' deactivated parent blocked','POST','/api/'+bp,b,http=400,status=5)
        # Real service outage must map to 500/status 0, not to a missing ID.
        processes[0].terminate();processes[0].wait(timeout=20)
        check(base,suite+' parent service offline','POST','/api/'+bp,b,http=500,status=0)
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
                try:process.wait(timeout=20)
                except subprocess.TimeoutExpired:process.kill();process.wait()
        for stream in streams:stream.close()


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--credentials',type=Path,help='Ignored local JSON with DB_USERNAME/DB_PASSWORD; do not commit it')
    args=parser.parse_args()
    credentials=json.loads(args.credentials.read_text()) if args.credentials else {}
    try:
        run_suite('DeThiSampleNew','User','Post','MediaGateway',credentials)
        run_suite('DeThi_SU26','Room','Reservation','HotelGateway',credentials)
    finally:
        out=ROOT/'test-results';out.mkdir(exist_ok=True)
        (out/'http-results.json').write_text(json.dumps({'executedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'checksPassed':len(RESULTS),'testPrefix':PREFIX,'checks':RESULTS},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('PASS all',len(RESULTS),'HTTP/OpenAPI/CORS checks')

if __name__=='__main__':main()
