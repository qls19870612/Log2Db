#!/bin/bash
sftp root@134.175.21.98 <<EOF
cd /data/project/server/javas/
lcd D:/workspace/Log2Db/
put -r -P log2db.jar
put -r -P tlog.xml
exit
close
bye
EOF