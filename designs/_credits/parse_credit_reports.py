import os
import shutil
import time

####################################################################################################
############## MAIN PROGRAM START ##################################################################
####################################################################################################

credits_range = range(1,10,1)
report_fname = "credits_report.csv"

# CHANGE THESE #
num_master = 15
slave_modnum=6
master_modnums=[11,31,19,17,15,13,21,1,29,3,27,5,25,23,9]
# THAT'S IT! #

master_constrings = ["X"]*num_master

for i in range(num_master):
    master_constrings[i] = str(master_modnums[i])+"_"+str(slave_modnum)

report_file = open(report_fname,'w')

 
print>>report_file,"num_credits;slave_xput;",

for i in range(num_master):
    print>>report_file,"master"+str(i)+"_latency;",
    
print>>report_file,"avg_latency;"
    
for num_credits in credits_range:
    print "parsing num_credits = "+str(num_credits)
    
    #------------------------------------------------------------
    # replace the number of credits in the tb file
    #------------------------------------------------------------
    
    credit_fname = "credits_"+str(num_credits)+".txt"
    
    #open old and new tb files
    credit_file = open(credit_fname,'r')
    
    outline = str(num_credits)
    
    conn_latencies = [0]*num_master
    
    for line in credit_file:
        part_list = line.split(";")
        if "module" in line:
            modnum = int(part_list[0].split("=")[1])
            xput = float(part_list[1].split("=")[1])
            if(modnum == slave_modnum):
                outline = outline+"; "+str(xput)
        elif "connection" in line:
            constring = part_list[0].split("=")[1]
            latency = float(part_list[1].split("=")[1])
            for i in range(num_master):
                if constring == master_constrings[i]:
                    conn_latencies[i] = latency
    
    avg_lat = float(sum(conn_latencies)/num_master)
    
    for i in range(num_master):
        outline = outline + "; "+str(conn_latencies[i])
    outline = outline + "; "+ str(avg_lat)
    
    credit_file.close()
    
    print>>report_file,outline
    
report_file.close()
    
    