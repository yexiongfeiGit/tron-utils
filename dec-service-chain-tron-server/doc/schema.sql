drop database if exists dec_service_chain_tron_testnet;
create database dec_service_chain_tron_testnet character set utf8mb4;
use dec_service_chain_tron_testnet;

-- 处理块
create table block_info (
  id int not null auto_increment,
  block_hash varchar(100) not null comment '区块Hash',
  block_number bigint not null comment '块高度',
  parent_hash varchar(100) not null comment '上一个块Hash',
  node_name varchar(100) not null comment '节点名',
  status tinyint not null comment '状态 0:待处理; 1:处理中; 2:处理失败; 3:处理完成; 4:异常数据',
  seize_count int not null comment '抢占处理权次数',
  fail_push_count int not null comment '失败次数',
  fork_dt bigint not null comment '分叉时间',
  block_dt bigint not null comment '出块时间',
  update_dt bigint not null comment '更新时间',
  dt bigint not null comment '创建时间',
  primary key (id),
  unique key uq_block_hash(block_hash),
  unique key uq_block_number(block_number),
  unique key uq_parent_hash(parent_hash),
  key idx_dt(dt)
) engine=InnoDB comment '处理块';


-- 币种
create table currency (
  id int not null auto_increment,
  unit varchar(10) not null comment '币种名称',
  contract_address varchar(100) not null comment '合约地址',
  decimals int not null comment '币种合约精度',
  dt bigint not null comment '创建时间',
  status tinyint not null comment'状态(0:失败,1正常)',
  remark varchar(64) not null comment"信息",
  primary key (id),
  unique key uq_contract_address(contract_address)
) engine=InnoDB comment='币种';

-- 订阅地址
create table subscribe_address (
  id int not null auto_increment,
  address varchar(100) not null comment '订阅地址',
  dt bigint not null comment '创建时间',
  primary key (id),
  unique key uq_address(address)
) engine=InnoDB comment '订阅地址';

-- 订阅全部币种地址
create table subscribe_application_address (
  id int not null auto_increment,
  app_num varchar(50) not null comment '订阅项目编号',
  subscribe_address_id int not null comment '订阅地址id',
  dt bigint not null comment '创建时间',
  primary key (id),
  unique key uq_app_num_subscribe_address_id(app_num, subscribe_address_id)
) engine=InnoDB comment '订阅全部币种地址';

-- -- 订阅地址下的某个币种
-- create table subscribe_address_currency (
--   id int not null auto_increment,
--   subscribe_address_id int not null comment '订阅地址id',
--   unit varchar(10) not null comment '币种名称',
--   contract_address varchar(100) not null comment '合约地址',
--   dt bigint not null comment '创建时间',
--   primary key (id)
-- ) engine=InnoDB comment '订阅地址下的某个币种';
--
-- -- 订阅地址下的某个币种关联订阅项目
-- create table subscribe_application_address_currency (
--   id int not null auto_increment,
--   app_num varchar(50) not null comment '订阅项目编号',
--   subscribe_address_currency_id int not null comment '订阅地址下的某个币种id',
--   dt bigint not null comment '创建时间',
--   primary key (id),
--   unique key uq_app_num_subscribe_address_currency_id(app_num, subscribe_address_currency_id)
-- ) engine=InnoDB comment '订阅地址下的某个币种关联订阅项目';
