"""Verify TODO0–5 using real SQL Server and directly addressed services.

Build all four applications first. Supply DB_PASSWORD and JWT_SECRET via the
environment (optional DB_USERNAME, DB_HOST, DB_PORT), or --credentials pointing
to an ignored local JSON file. No database reset or login changes are made.
"""
import argparse
import concurrent.futures
import json
import os
from pathlib import Path
import socket
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT.parent / '.work'
CHECKS = []


def request(port, method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f'http://localhost:{port}' + path, data=data,
        method=method, headers={'Content-Type': 'application/json'})
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as ex:
        response = ex
    raw = response.read()
    try:
        value = json.loads(raw)
    except ValueError:
        value = raw.decode(errors='replace')
    return response.status, value


def check(label, condition):
    assert condition, label
    CHECKS.append({'scenario': label, 'passed': True})
    print('PASS', label, flush=True)


def sql(query):
    # Read-only evidence queries use the caller's Windows SQL administrator.
    result = subprocess.run(['sqlcmd', '-S', 'localhost', '-E', '-C', '-b', '-h', '-1', '-W'],
        input='SET NOCOUNT ON;\n' + query + '\nGO\n', text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr + result.stdout)
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--credentials', type=Path)
    parser.add_argument('--newman-js', type=Path, help='Optional local Newman JS entrypoint')
    args = parser.parse_args()
    WORK.mkdir(exist_ok=True)
    env = dict(os.environ)
    if args.credentials:
        env.update(json.loads(args.credentials.read_text(encoding='utf-8')))
    for key in ['DB_PASSWORD', 'JWT_SECRET']:
        if not env.get(key):
            raise RuntimeError(key + ' must be supplied')
    specs = [('MSAccount', 'ms-account', 8081), ('MSBrand', 'ms-brand', 8082),
        ('MSWatch', 'ms-watch', 8083), ('APIGateway', 'api-gateway', 8080)]
    processes, streams = {}, []
    for _, _, port in specs:
        with socket.socket() as sock:
            if sock.connect_ex(('localhost', port)) == 0:
                raise RuntimeError(f'Port {port} already in use; leave unrelated apps untouched')

    def start(name, artifact):
        folder = ROOT / (name + '_DE190468')
        jar = folder / 'target' / (artifact + '-1.0.0.jar')
        stream = (WORK / (name + '-runtime.log')).open('a', encoding='utf-8')
        streams.append(stream)
        process = subprocess.Popen(['java', '-Xmx256m', '-jar', str(jar)], cwd=folder,
            env=env, stdout=stream, stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
        processes[name] = process
        return process

    def ready(name, port):
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            if processes[name].poll() is not None:
                raise RuntimeError(name + ' exited; inspect ignored runtime log')
            try:
                status, _ = request(port, 'GET', '/v3/api-docs' if port != 8080 else '/not-a-route')
                if status == (404 if port == 8080 else 200):
                    return
            except (OSError, urllib.error.URLError):
                pass
            time.sleep(0.5)
        raise RuntimeError(name + ' startup timed out')

    try:
        for name, artifact, port in specs:
            start(name, artifact)
            ready(name, port)
            check(name + ' starts on ' + str(port), True)
        for port in [8081, 8082, 8083]:
            status, body = request(port, 'GET', '/swagger-ui.html')
            check('Swagger enabled on ' + str(port), status == 200 and 'swagger' in body.lower())
        check('TODO6 watch API is not prematurely implemented', request(8083, 'GET', '/api/watches')[0] == 404)
        check('TODO7 gateway does not expose internal brand routes', request(8080, 'PUT', '/internal/brands/1/watches/99')[0] == 404)
        names = sql("SELECT name FROM DBAccount.sys.tables; SELECT name FROM DBBrand.sys.tables;")
        check('Exact code-first table names', all(x in names.splitlines() for x in ['StoreAccounts', 'WatchBrands', 'BrandWatches']))
        columns = sql("SELECT name FROM DBAccount.sys.columns WHERE object_id=OBJECT_ID('DBAccount.dbo.StoreAccounts'); SELECT name FROM DBBrand.sys.columns WHERE object_id=OBJECT_ID('DBBrand.dbo.BrandWatches');")
        check('Exact AccountID WatchID BrandID columns', all(x in columns.splitlines() for x in ['AccountID', 'WatchID', 'BrandID']))
        check('Five BCrypt accounts', sql("SELECT COUNT(*) FROM DBAccount.dbo.StoreAccounts WHERE Password LIKE '$2a$%';") == '5')
        check('Exactly ten initial brand links', sql('SELECT COUNT(*) FROM DBBrand.dbo.BrandWatches;') == '10')
        check('DBWatch schema remains pending TODO6', sql('SELECT COUNT(*) FROM DBWatch.sys.tables;') == '0')

        if args.newman_js:
            report = WORK / 'newman-full.json'
            result = subprocess.run(['node', str(args.newman_js.resolve()), 'run',
                str(ROOT / 'postman/Assignment2_TODO5.postman_collection.json'), '--reporters', 'json',
                '--reporter-json-export', str(report), '--timeout-request', '10000'],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace')
            data = json.loads(report.read_text(encoding='utf-8'))
            check('Postman collection has zero failures', result.returncode == 0 and not data['run']['failures'])
            stats = data['run']['stats']
            print('Newman:', stats['requests']['total'], 'requests,', stats['assertions']['total'], 'assertions', flush=True)
        else:
            stats = None

        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
            results = list(executor.map(lambda _: request(8082, 'PUT', '/internal/brands/1/watches/99'), range(8)))
        check('Concurrent first assign returns success for all callers', all(status == 200 for status, _ in results))
        check('Concurrent assign inserts only one SQL row', sql('SELECT COUNT(*) FROM DBBrand.dbo.BrandWatches WHERE WatchID=99;') == '1')
        # Reassign concurrently too: state must remain one row, whichever call wins.
        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
            results = list(executor.map(lambda b: request(8082, 'PUT', f'/internal/brands/{b}/watches/99'), [1, 2, 1, 2, 1, 2]))
        check('Concurrent reassign remains idempotent without unique failures', all(status == 200 for status, _ in results) and sql('SELECT COUNT(*) FROM DBBrand.dbo.BrandWatches WHERE WatchID=99;') == '1')
        request(8082, 'DELETE', '/internal/brands/watches/99')
        request(8082, 'DELETE', '/internal/brands/watches/1')
        processes['MSBrand'].terminate()
        processes['MSBrand'].wait(timeout=20)
        start('MSBrand', 'ms-brand')
        ready('MSBrand', 8082)
        check('Restart does not recreate removed seed assignments', sql('SELECT COUNT(*) FROM DBBrand.dbo.BrandWatches WHERE WatchID=1;') == '0')
        request(8082, 'PUT', '/internal/brands/1/watches/1')
        check('Seed data restored after smoke tests', sql('SELECT COUNT(*) FROM DBBrand.dbo.BrandWatches;') == '10')
        status, body = request(8081, 'POST', '/api/auth/login', {'username': 'adminsys', 'password': '@1'})
        check('Login after all tests', status == 200 and body['data']['role'] == 1)
        tokens = body['data']
        check('Tokens remain local and are not written to evidence', bool(tokens['accessToken']))
        account_log = (WORK / 'MSAccount-runtime.log').read_text(encoding='utf-8')
        brand_log = (WORK / 'MSBrand-runtime.log').read_text(encoding='utf-8')
        check('Account seed log records five accounts', 'Seeded 5 accounts' in account_log)
        check('Participant logs show assign and unassign', '[SAGA-PARTICIPANT] ASSIGN' in brand_log and '[SAGA-PARTICIPANT] UNASSIGN' in brand_log)
        output = ROOT / 'test-results'
        output.mkdir(exist_ok=True)
        (output / 'todo5-runtime.json').write_text(json.dumps({
            'executedAt': datetime.now(timezone.utc).isoformat(), 'passed': len(CHECKS),
            'checks': CHECKS, 'newman': stats,
            'sqlTables': names.splitlines(), 'sqlColumns': columns.splitlines(),
            'scope': 'TODO0 through TODO5; no orchestrator or gateway routing yet'
        }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    finally:
        for process in reversed(list(processes.values())):
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
        for stream in streams:
            stream.close()


if __name__ == '__main__':
    main()
