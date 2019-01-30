#!/bin/bash
sftp root@192.168.1.99 <<EOF
cd /data/svr_log/1
lcd D:/workspace/Log2Db/1
get -r ./*
exit
close
bye
EOF