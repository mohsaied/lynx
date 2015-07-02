import os
import shutil
import time

####################################################################################################
############## MAIN PROGRAM START ##################################################################
####################################################################################################

num_masters      = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
baseline_credits = [9, 6, 4, 4, 3, 2, 2, 2,  2,  1,  1,  1,  1,  1]


credits_range = range(1,10,1)
report_fname = "credits_report.csv"

# CHANGE THESE #
num_master = 15
slave_modnum=6
master_modnums=[11,31,19,17,15,13,21,1,29,3,27,5,25,23,9]
# THAT'S IT! #

base_cred = baseline_credits[num_master-2]

master_constrings = ["X"]*num_master

for i in range(num_master):
    master_constrings[i] = str(master_modnums[i])+"_"+str(slave_modnum)

latency_report_file = open("latency_"+report_fname,'w')
xput_report_file = open("xput_"+report_fname,'w')

 
print>>latency_report_file,"num_credits\tavg_latency\t",
for master in master_modnums:
    print>>latency_report_file,"master"+str(master)+"_latency\t",
print>>latency_report_file,""

print>>xput_report_file,"num_credits\tavg_xput\t",
for master in master_modnums:
    print>>xput_report_file,"master"+str(master)+"_xput\t",
print>>xput_report_file,""
    
    
for num_credits in credits_range:
    print "parsing num_credits = "+str(num_credits)
    
    #------------------------------------------------------------
    # replace the number of credits in the tb file
    #------------------------------------------------------------
    
    credit_fname = "credits_"+str(num_credits*base_cred)+".txt"
    
    #open old and new tb files
    credit_file = open(credit_fname,'r')
    
    latency_outline = str(num_credits)
    xput_outline = str(num_credits)
    
    conn_latencies = [0]*num_master
    conn_xputs = [0.0]*num_master
    
    for line in credit_file:
        part_list = line.split(";")
        if "module" in line:
            modnum = int(part_list[0].split("=")[1])
            xput = float(part_list[1].split("=")[1])
            for i in range(num_master):
                if(modnum == master_modnums[i]):
                    conn_xputs[i]=xput
        elif "connection" in line:
            constring = part_list[0].split("=")[1]
            latency = float(part_list[1].split("=")[1])
            for i in range(num_master):
                if constring == master_constrings[i]:
                    conn_latencies[i] = latency
    
    credit_file.close()
    
    avg_lat = float(sum(conn_latencies)/num_master)
    latency_outline = latency_outline + "\t "+ str(avg_lat)
    for i in range(num_master):
        latency_outline = latency_outline + "\t "+str(conn_latencies[i])
    
    avg_xput = float(sum(conn_xputs)/num_master) 
    xput_outline = xput_outline + "\t "+ str(avg_xput)
    for i in range(num_master):
        xput_outline = xput_outline + "\t "+str(conn_xputs[i])
    
    
    print>>latency_report_file,latency_outline
    print>>xput_report_file,xput_outline
    
latency_report_file.close()
xput_report_file.close()
    
    