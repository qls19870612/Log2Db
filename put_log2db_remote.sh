#!/bin/bash
sftp root@134.175.127.247 <<EOF
cd /project/server/javas/
lcd D:/workspace/Log2Db/
put -r -P log2db.jar
put -r -P tlog.xml
exit
close
bye
EOF