import os
import shutil
import time

#function to check a report file for a crash
def check_for_crash(report_path):
    #if report file isnt there, then there was a problem
    if not os.path.isfile(report_path):
        return True
    #if report file exists, we'll check if it has the crash secret word
    else:
        rpt = open(report_path,'r')
        for line in rpt:
            if "SCHMETTERLING" in line:
                rpt.close()
                return True
    rpt.close()
    return False

#find all the metrics in metric_names list from the report path specified
#and return a list of their values in corresponding order
# -1 anywhere indicates not found
def find_metrics(report_path, metric_names):
    #init return vector to -1's 
    metric_values = ["-"]*len(metric_names)

    #parse the file
    rpt = open(report_path,'r')
    for line in rpt:
        for i in range(0,len(metric_names)):
            if metric_names[i] in line:
                part_list = line.split()
                metric_values[i] = part_list[2]
    rpt.close()
    
    return metric_values

def find_golden(golden_path, metric_names):
    #init return vector to -1's 
    golden_values = ["-"]*len(metric_names)
    #if golden file doesnt exist return emptys
    if not os.path.isfile(golden_path):
        return golden_values
    #parse the file
    golden = open(golden_path,'r')
    for line in golden:
        for i in range(0,len(metric_names)):
            if metric_names[i] in line:
                part_list = line.split()
                golden_values[i] = part_list[2]
    golden.close()
    return golden_values
    
#header for html table
def init_web(web_dir, version, metric_names):
    #create a new webfile
    web = open(web_dir+str(version)+".html",'w')
    
    #write the file preamble
    print >>web, "<!DOCTYPE html>\n\
<html>\n\
<head>\n\
<style>\n\
table, th, td {\n\
    border: 1px solid black;\n\
    border-collapse: collapse;\n\
}\n\
th, td {\n\
    padding: 5px;\n\
    text-align: center;\n\
}\n\
</style>\n\
</head>\n\
<body>\n\
<h2> LYNX summary v"+str(version)+"</h2>\n\
<table>\n\
<tr>\n"

    #now write the metric names in the headers
    print >>web, "<th>Test Name</th>"
    for metric in metric_names:
        print >>web, "<th>"+metric+"</th>"
    print >>web, "</tr>"
    
    web.close()

#footer for html table
def close_web(web_dir, version):
    web = open(web_dir+str(version)+".html",'a')
    #write the file postamble
    print >>web, "</table>\n\
</body>\n\
</html>\n"
    web.close()

def update_web(web_dir, version, test_name, crash, metric_values, golden_values):
    web = open(web_dir+str(version)+".html",'a')
    print >>web, "<th>"+test_name+"</th>"
    if crash:
        print >>web, "<td bgcolor=\"#FF0000\" colspan=\""+str(len(metric_values))+"\">CRASH</td>"
    else:
        for i in range(0,len(metric_values)):
            metric = metric_values[i]
            golden = golden_values[i]
            if metric == '-':
                print >>web, "<td bgcolor=\"#C0C0C0\">"+metric+"</td>"
            else:
                if golden == '-' or metric == golden:
                    print >>web, "<td bgcolor=\"#00FF00\">"+metric+"</td>"
                else:
                    print >>web, "<td bgcolor=\"#FFFF00\">"+metric+"</td>"
                    
    print >>web, "</tr>"
    web.close()
    
####################################################################################################
############## MAIN PROGRAM START ##################################################################
####################################################################################################
    
java_cmd_win = "java -cp \"D:\\Dropbox\\PhD\\Software\\noclynx;D:\\Dropbox\\PhD\\Software\\noclynx\\jfreechart\\jcommon-1.0.23\\jcommon-1.0.23.jar;D:\\Dropbox\\PhD\\Software\\noclynx\\jfreechart\\jfreechart-1.0.19\\lib\\jfreechart-1.0.19.jar;D:\\Dropbox\\PhD\\Software\\noclynx\\jgraphx\\jgraphx.jar\" lynx.main.Main -c "

#version number of the program
#increment version number with each set of major changes
version = 1

tests_dir = "D:\\Dropbox\\PhD\\Software\\noclynx\\designs\\"
reports_dir = "archive/"+str(version)+"/"
web_dir= "web/"

test_names = [
    "chain",
    "chain_big",
    "invalid",
    "quadratic",
    "ram",
    "ram_big",
    "ram_bidir",
    "ram_chain",
    "tarjan",
]

metric_names = [
    "num_connections",
    "num_modules",
    "num_clusters",
    "map_cost",
    "max_link_util",
    "noc_bw_util",
]

#initialize web file
init_web(web_dir, version, metric_names)


#*******************************************************************************************
# Run/parse all the tests
#*******************************************************************************************
for test_name in test_names:
    
    curr_test_path = tests_dir+test_name+"\\"+test_name+".xml"
    golden_path = tests_dir+test_name+"\\golden.txt"

    #------------------------------------------------------------------
    #run test
    #------------------------------------------------------------------
    os.system(java_cmd_win+curr_test_path)
    
    #------------------------------------------------------------------
    #copy report
    #------------------------------------------------------------------
    if not os.path.isdir("archive/"+str(version)):
        os.mkdir("archive/"+str(version))
    shutil.copy2(curr_test_path+".rpt", "archive/"+str(version)+"/")
    
    #------------------------------------------------------------------
    #parse report
    #------------------------------------------------------------------
    #path to current report file
    report_path = reports_dir+test_name+".xml.rpt"
    
    #first check for crash or non-existent report file
    crash = check_for_crash(report_path)
    
    #list of returned metrics
    if not crash:
        metric_values = find_metrics(report_path, metric_names)
        golden_values = find_golden(golden_path, metric_names)
    
    #------------------------------------------------------------------
    #update test page
    #------------------------------------------------------------------
    update_web(web_dir, version, test_name, crash, metric_values, golden_values)
    
    
close_web(web_dir, version)