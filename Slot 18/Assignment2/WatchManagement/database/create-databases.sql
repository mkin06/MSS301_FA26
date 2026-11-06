-- [TODO-2.1] Run with an existing SQL Server administrator.
-- Leave existing databases, logins and passwords unchanged.
IF DB_ID(N'DBAccount') IS NULL CREATE DATABASE DBAccount;
GO
IF DB_ID(N'DBBrand') IS NULL CREATE DATABASE DBBrand;
GO
IF DB_ID(N'DBWatch') IS NULL CREATE DATABASE DBWatch;
GO
