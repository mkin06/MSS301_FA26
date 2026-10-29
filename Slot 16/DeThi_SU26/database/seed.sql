USE [MSS301_2026_PE];
GO
IF NOT EXISTS (SELECT 1 FROM rooms WHERE room_number=N'A-101')
INSERT INTO rooms(room_number,room_type,price_per_night,capacity,floor,status) VALUES(N'A-101',N'DOUBLE',850000,2,1,N'AVAILABLE');
IF NOT EXISTS (SELECT 1 FROM rooms WHERE room_number=N'A-102')
INSERT INTO rooms(room_number,room_type,price_per_night,capacity,floor,status) VALUES(N'A-102',N'SINGLE',500000,1,1,N'AVAILABLE');
IF NOT EXISTS (SELECT 1 FROM rooms WHERE room_number=N'B-201')
INSERT INTO rooms(room_number,room_type,price_per_night,capacity,floor,status) VALUES(N'B-201',N'SUITE',1500000,4,2,N'MAINTENANCE');
GO
