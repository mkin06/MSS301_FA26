USE [MSS301_2026_PE_MEDIA];
GO
IF NOT EXISTS (SELECT 1 FROM users WHERE username=N'creator_a')
INSERT INTO users(username,email,role,rate_per_post,max_daily_posts,level,status) VALUES(N'creator_a',N'a@example.com',N'CREATOR',500000,3,1,N'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM users WHERE username=N'creator_b')
INSERT INTO users(username,email,role,rate_per_post,max_daily_posts,level,status) VALUES(N'creator_b',N'b@example.com',N'CREATOR',300000,1,1,N'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM users WHERE username=N'creator_c')
INSERT INTO users(username,email,role,rate_per_post,max_daily_posts,level,status) VALUES(N'creator_c',N'c@example.com',N'CREATOR',800000,4,2,N'SUSPENDED');
GO
