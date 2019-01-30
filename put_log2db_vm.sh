#!/bin/bash
sftp root@192.168.1.99 <<EOF
cd /project/server/javas/
lcd D:/workspace/Log2Db/
put -r -P log2db.jar
put -r -P tlog.xml
exit
close
bye
EOF