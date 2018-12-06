#!/bin/bash
#SFTP配置信息
#IP
IP=192.168.1.99
#端口
PORT=22
#用户名
USER=root
#密码
PASSWORD=song
#待上传文件根目录
CLIENTDIR=D:/workspace/Log2Db
#SFTP目录
SEVERDIR=/home/javas
#待上传文件名
FILE=Log2Db.jar

#lftp -u ${USER},${PASSWORD} sftp://${IP}:${PORT} <<EOF
curl -v --insecure sftp://${USER}:${PASSWORD}@192.168.1.99
cd ${SEVERDIR}/
lcd ${CLIENTDIR}
put ${FILE}
by
EOF
