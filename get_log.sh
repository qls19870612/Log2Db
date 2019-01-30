!/bin/bash
rp=/data/svr_log/1/
p=`pwd`
p=$(echo ${p:1:1}:/${p:3}/1)
echo $p
sftp root@134.175.21.98 <<EOF
cd ${rp}
lcd ${p}
get -r -P ./
exit
close
bye
EOF

# rp=/data/svr_log/1/
# p=/data/svr_log/1/

# sftp root@134.175.21.98 <<EOF
# cd ${rp}
# lcd ${p}
# get -r -P ./*
# exit
# close
# bye
# EOF
