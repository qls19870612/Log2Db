/*
MySQL Backup
Source Server Version: 5.5.60
Source Database: dbdiablomuzhilog
Date: 2019/6/19/周三 09:47:35
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
--  Procedure definition for `addDayPartition`
-- ----------------------------
DROP PROCEDURE IF EXISTS `addDayPartition`;
DELIMITER ;;
CREATE DEFINER=`root`@`127.0.0.1` PROCEDURE `addDayPartition`(IN_SCHEMANAME VARCHAR(64), IN_TABLENAME VARCHAR(64),AFTER_DAY INT(11),out result varchar(200))
BEGIN
	-- addDayPartition
		DECLARE ROWS_CNT INT UNSIGNED;
    DECLARE BEGINTIME TIMESTAMP;
    DECLARE ENDTIME TIMESTAMP;
    DECLARE DAYS_TIME INT;
    DECLARE PARTITIONNAME VARCHAR(16);
    
    declare days int;

    IF AFTER_DAY < 0 THEN
			SET days = AFTER_DAY;
			SET AFTER_DAY = 0;
		ELSE
			SET days = 0;
		END IF;
    while days < AFTER_DAY DO
			SET BEGINTIME = NOW() + INTERVAL days DAY;
			
			SET ENDTIME = BEGINTIME + INTERVAL 1 DAY;
 
	 
			SET PARTITIONNAME = DATE_FORMAT(BEGINTIME, 'p_%Y%m%d');
			SET DAYS_TIME = TO_DAYS(BEGINTIME);

			SELECT COUNT(*)
			INTO ROWS_CNT
			FROM information_schema.partitions
			WHERE table_schema = IN_SCHEMANAME AND table_name = IN_TABLENAME AND partition_name = PARTITIONNAME;
			IF ROWS_CNT = 0 THEN
					SET @SQL = CONCAT('ALTER TABLE `', IN_SCHEMANAME, '`.`', IN_TABLENAME, '`',' ADD PARTITION (PARTITION ', PARTITIONNAME, ' VALUES IN (', DAYS_TIME, '))');
					PREPARE STMT FROM @SQL;
					EXECUTE STMT;
					DEALLOCATE PREPARE STMT;
			END IF;

			set days = days+1;

		end while;
 END
;;
DELIMITER ;

-- ----------------------------
--  Procedure definition for `initDayPartition`
-- ----------------------------
DROP PROCEDURE IF EXISTS `initDayPartition`;
DELIMITER ;;
CREATE DEFINER=`root`@`127.0.0.1` PROCEDURE `initDayPartition`(IN_SCHEMANAME VARCHAR(64), IN_TABLENAME VARCHAR(64))
BEGIN
	#Routine body goes here... initDayPartition
		declare days int;
    DECLARE BEGINTIME TIMESTAMP;
		DECLARE PARTITIONNAME VARCHAR (20);
		DECLARE DAY_TIME VARCHAR (20);
		DECLARE ADD_PARTITION VARCHAR (100);
		DECLARE CONTINUE HANDLER FOR SQLSTATE '01505' SET days=100;
		
		SET BEGINTIME = NOW();
		SET DAY_TIME = TO_DAYS(BEGINTIME);
		SET PARTITIONNAME = DATE_FORMAT(BEGINTIME, 'p_%Y%m%d');
    
		
		SET ADD_PARTITION = CONCAT(' (partition ',PARTITIONNAME,' VALUES IN (',DAY_TIME,'))');
		SET @SQL=CONCAT('alter table ',IN_TABLENAME,' partition by list (to_days(dt))',ADD_PARTITION);
		PREPARE STMT FROM @SQL;
		EXECUTE STMT;
		DEALLOCATE PREPARE STMT;
		
END
;;
DELIMITER ;

-- ----------------------------
--  Procedure definition for `operationAllPartition`
-- ----------------------------
DROP PROCEDURE IF EXISTS `operationAllPartition`;
DELIMITER ;;
CREATE DEFINER=`root`@`127.0.0.1` PROCEDURE `operationAllPartition`()
BEGIN


DECLARE done INT DEFAULT 0; /*用于判断是否结束循环*/
DECLARE cur VARCHAR(200);/*存储表名称的变量*/
DECLARE sechema_name VARCHAR(30) default 'dbdiablomuzhilog';
declare result varchar(200);
declare t VARCHAR(100);
declare a INT;
DECLARE tbs_list CURSOR FOR SELECT TABLE_NAME FROM information_schema.`TABLES` WHERE TABLE_Schema = sechema_name;
declare continue handler for not FOUND set done = 1; /*done = true;*/

-- DECLARE continue HANDLER FOR SQLEXCEPTION SET a=1;

delete from dbdiabloconf.log;
OPEN tbs_list;
REPEAT
FETCH tbs_list INTO cur;
if not done THEN
	insert into `dbdiabloconf`.`log` (`value`) values(cur);
	-- CALL `dbdiablomuzhilog`.`removeDayPartition`(sechema_name,cur); -- 删除分区
	-- CALL `dbdiablomuzhilog`.`initDayPartition`(sechema_name,cur); -- 初始化分区
	
	CALL `dbdiablomuzhilog`.`addDayPartition`(sechema_name,cur,10,@result);-- 当前时间往后添加10天分区
	CALL `dbdiablomuzhilog`.`addDayPartition`(sechema_name,cur,-10,@result); -- 当前时间往前添加10天分区
	-- CALL `dbdiablomuzhilog`.`removeAllData`(sechema_name,cur);
end if;
until done end repeat;
CLOSE tbs_list;

END
;;
DELIMITER ;

-- ----------------------------
--  Procedure definition for `removeAllData`
-- ----------------------------
DROP PROCEDURE IF EXISTS `removeAllData`;
DELIMITER ;;
CREATE DEFINER=`root`@`127.0.0.1` PROCEDURE `removeAllData`(IN_SCHEMANAME VARCHAR(64), IN_TABLENAME VARCHAR(64))
BEGIN
	#Routine body goes here...
		SET @SQL=CONCAT('delete from ',IN_SCHEMANAME ,'.',IN_TABLENAME);
		PREPARE STMT FROM @SQL;
		EXECUTE STMT;
		DEALLOCATE PREPARE STMT;
END
;;
DELIMITER ;

-- ----------------------------
--  Procedure definition for `removeDayPartition`
-- ----------------------------
DROP PROCEDURE IF EXISTS `removeDayPartition`;
DELIMITER ;;
CREATE DEFINER=`root`@`127.0.0.1` PROCEDURE `removeDayPartition`(IN_SCHEMANAME VARCHAR(64), IN_TABLENAME VARCHAR(64))
BEGIN
	#Routine body goes here... removeDayPartition
		declare days int;
     DECLARE CONTINUE HANDLER FOR SQLSTATE '01505' SET days=100;
		
		 SET @SQL = CONCAT('alter table `',IN_SCHEMANAME,'`.`',IN_TABLENAME,'` remove partitioning');
					PREPARE STMT FROM @SQL;
					EXECUTE STMT;
					DEALLOCATE PREPARE STMT;
		
END
;;
DELIMITER ;

-- ----------------------------
--  Records 
-- ----------------------------
