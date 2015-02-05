while true
do
    echo "$(date '+%Y-%m-%d %H:%M:%S'),$(ls -1 journal | wc -l),$(du journal | cut -f1),$(ls -1 snapshots | wc -l),$(du snapshots | cut -f1)" | tee -a file_bloat_logfile.csv
    sleep 2
done
