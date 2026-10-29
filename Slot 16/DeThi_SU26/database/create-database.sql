IF DB_ID(N'MSS301_2026_PE') IS NULL CREATE DATABASE [MSS301_2026_PE];
GO


/* Table: rooms
   Target: MS SQL Server 2016
*/
USE [MSS301_2026_PE]
GO

CREATE TABLE [dbo].[rooms] (
    [room_id]         INT              IDENTITY(1,1) NOT NULL, -- Auto increment
    [room_number]     NVARCHAR(20)     NOT NULL,               -- Unique
    [room_type]       NVARCHAR(10)     NOT NULL,
    [price_per_night] DECIMAL(18,2)    NOT NULL,
    [capacity]        INT              NOT NULL,
    [floor]           INT              NOT NULL,
    [status]          NVARCHAR(15)     NOT NULL,

    -- Primary Key
    CONSTRAINT [PK_Rooms] PRIMARY KEY CLUSTERED ([room_id] ASC),

    -- Unique room number
    CONSTRAINT [UQ_Rooms_Number] UNIQUE NONCLUSTERED ([room_number] ASC),

    -- Room type allowed values
    CONSTRAINT [CK_Rooms_Type]
        CHECK ([room_type] IN ('SINGLE', 'DOUBLE', 'SUITE', 'DELUXE')),

    -- Price must be greater than 0
    CONSTRAINT [CK_Rooms_Price]
        CHECK ([price_per_night] > 0),

    -- Capacity between 1 and 10
    CONSTRAINT [CK_Rooms_Capacity]
        CHECK ([capacity] >= 1 AND [capacity] <= 10),

    -- Floor must be >= 1
    CONSTRAINT [CK_Rooms_Floor]
        CHECK ([floor] >= 1),

    -- Status allowed values
    CONSTRAINT [CK_Rooms_Status]
        CHECK ([status] IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE'))
);
GO

USE [MSS301_2026_PE]
GO

/****** Object:  Table [dbo].[reservations]    Script Date: 6/29/2026 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[reservations](
    [reservation_id]   [int]           IDENTITY(1,1) NOT NULL,
    [guest_name]       [nvarchar](100) NOT NULL,
    [guest_email]      [nvarchar](100) NOT NULL,
    [guest_phone]      [nvarchar](20)  NOT NULL,
    [check_in_date]    [date]          NOT NULL,
    [check_out_date]   [date]          NOT NULL,
    [number_of_guests] [int]           NOT NULL,
    [total_amount]     [decimal](18,2) NOT NULL,
    [status]           [nvarchar](15)  NOT NULL,
    [room_id]          [int]           NOT NULL,

 CONSTRAINT [PK_Reservations] PRIMARY KEY CLUSTERED
(
    [reservation_id] ASC
) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
        ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON,
        OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- check_out_date must be after check_in_date
ALTER TABLE [dbo].[reservations]
    WITH CHECK ADD CONSTRAINT [CK_Reservations_Dates]
    CHECK ([check_out_date] > [check_in_date]);
GO

ALTER TABLE [dbo].[reservations] CHECK CONSTRAINT [CK_Reservations_Dates];
GO

-- number_of_guests must be >= 1
ALTER TABLE [dbo].[reservations]
    WITH CHECK ADD CONSTRAINT [CK_Reservations_Guests]
    CHECK ([number_of_guests] >= 1);
GO

ALTER TABLE [dbo].[reservations] CHECK CONSTRAINT [CK_Reservations_Guests];
GO

-- total_amount must be >= 0
ALTER TABLE [dbo].[reservations]
    WITH CHECK ADD CONSTRAINT [CK_Reservations_Amount]
    CHECK ([total_amount] >= 0);
GO

ALTER TABLE [dbo].[reservations] CHECK CONSTRAINT [CK_Reservations_Amount];
GO

-- Status allowed values
ALTER TABLE [dbo].[reservations]
    WITH CHECK ADD CONSTRAINT [CK_Reservations_Status]
    CHECK ([status] IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'));
GO

ALTER TABLE [dbo].[reservations] CHECK CONSTRAINT [CK_Reservations_Status];
GO

-- Foreign key to rooms
ALTER TABLE [dbo].[reservations]
    WITH CHECK ADD CONSTRAINT [FK_Reservations_Room]
    FOREIGN KEY ([room_id]) REFERENCES [dbo].[rooms]([room_id]);
GO

ALTER TABLE [dbo].[reservations] CHECK CONSTRAINT [FK_Reservations_Room];
GO
