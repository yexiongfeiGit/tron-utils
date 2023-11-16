


INSERT INTO `schedule_task` (`code`, `name`, `project_type`, `project_public_key`, `project_secret`, `call_url`, `cron_expression`, `description`, `status`, `call_dt`, `next_call_dt`, `dt`)
VALUES
	('dec-service-chain-tron-scan', 'tron区块扫描', 'dec-service-chain-tron-mainnet', 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANPKRvcPQXNSDV+rvaHu9H46pAQwdIlienOx9ZUAKWciXcMw+tlFQF30ACStYXrJHev+SGnbh9076FAUQ82zdu0CAwEAAQ==', 'dec-middleware-schedule', '/task/scan', '0/3 * * * * ?', 'tron区块扫描', 1, 1635232650347, 1635232660000, unix_timestamp(now())*1000);


INSERT INTO `schedule_task` (`code`, `name`, `project_type`, `project_public_key`, `project_secret`, `call_url`, `cron_expression`, `description`, `status`, `call_dt`, `next_call_dt`, `dt`)
VALUES
	('dec-service-chain-tron-testnet-scan-delete', 'tron区块扫描', 'dec-service-chain-tron-mainnet', 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANPKRvcPQXNSDV+rvaHu9H46pAQwdIlienOx9ZUAKWciXcMw+tlFQF30ACStYXrJHev+SGnbh9076FAUQ82zdu0CAwEAAQ==', 'dec-middleware-schedule', '/task/clearBlockInfoHistory', '0 0 0/4 * * ?', '删除较老数据', 1, 1635477801321, 1635477804000, unix_timestamp(now())*1000);

