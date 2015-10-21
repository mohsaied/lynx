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

    if os.path.isfile(report_path):
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
<A href="+str(version-1)+".html>previous</A> - <A href="+str(version+1)+".html>next</A>  \n\
<br><br><br>\n\
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

def update_web(web_dir, version, test_name, crash, metric_values, golden_values, previous_values):
    web = open(web_dir+str(version)+".html",'a')
    print >>web, "<th>"+test_name+"</th>"
    if crash:
        print >>web, "<td bgcolor=\"#FF0000\" colspan=\""+str(len(metric_values))+"\">CRASH</td>"
    else:
        for i in range(0,len(metric_values)):
            metric = metric_values[i]
            metric_fl = 0
            if metric != '-':
                metric_fl = float(metric)
            golden = golden_values[i]
            previous = previous_values[i]
            prev_fl = 0
            if previous != '-':
                prev_fl = float(previous)
            if metric == '-':
                print >>web, "<td>"+metric+"</td>"
            else:
                if previous == '-' or metric == previous or metric == '-':
                    if metric_fl == 0 and i == 5: #special case for zero quench
                        print >>web, "<td bgcolor=\"#D3D3D3\">"+metric+"</td>"
                    else:
                        print >>web, "<td bgcolor=\"#E0EEE0\">"+metric+"</td>"
                else:
                    if i==5 and metric_fl < prev_fl and (prev_fl - metric_fl) > 0.9:
                        print >>web, "<td bgcolor=\"#FFFF00\">"+metric+" ("+previous+")"+"</td>"
                    elif i==5 and metric_fl > prev_fl and (metric_fl - prev_fl) > 0.9:
                        print >>web, "<td bgcolor=\"#00FF00\">"+metric+" ("+previous+")"+"</td>"
                    elif metric_fl < prev_fl and (prev_fl - metric_fl) > 0.9:
                        print >>web, "<td bgcolor=\"#00FF00\">"+metric+" ("+previous+")"+"</td>"
                    elif metric_fl > prev_fl and (metric_fl - prev_fl) > 0.9:
                        print >>web, "<td bgcolor=\"#FFFF00\">"+metric+" ("+previous+")"+"</td>"
                    elif metric_fl == 0 and i == 5:
                        print >>web, "<td bgcolor=\"#D3D3D3\">"+metric+"</td>"
                    else:
                        print >>web, "<td bgcolor=\"#E0EEE0\">"+metric+"</td>"
                    
    print >>web, "</tr>"
    web.close()
    
####################################################################################################
############## MAIN PROGRAM START ##################################################################
####################################################################################################

platform = "windows"
#platform = "linux"

path_start = "D:"
path_seperator = "\\"
if platform == "linux":
    path_start = "/home/mohamed"
    path_seperator = "/"

java_cmd = "java -cp \""+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jcommon-1.0.23"+path_seperator+"jcommon-1.0.23.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jfreechart-1.0.19"+path_seperator+"lib"+path_seperator+"jfreechart-1.0.19.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jgraphx"+path_seperator+"jgraphx.jar\" lynx.main.Main -c "

#if run is set to true, we run through the latest code base
#if it is set to false, then we simply compare two results directories
run = True

#version number of the program
#increment version number with each set of major changes
version = 15

tests_dir = path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"designs"+path_seperator+""
reports_dir = "archive/"+str(version)+"/"
prev_reports_dir = "archive/"+str(version-1)+"/"
web_dir= "web/"

test_names = [
    "chain_n4_w128",
    "chain_n16_w128",
    "chain_n20_w128",
    
    "multimaster_n3_w128",
    "multimaster_n9_w128",
    "multimaster_n17_w128",
    
    "xbar_n4_w128",
    #"xbar_n16_w128",
    #"xbar_n64_w128",
    
    "gzip",
    
    "broadcast_n3_w128",
    "broadcast_n9_w128",
    "broadcast_n16_w128",
    
    "converge_n3_w128",
    "converge_n9_w128",
    "converge_n18_w128",
    
    "reconverge_n2_w128",
    "reconverge_n4_w128",
    "reconverge_n6_w128",
    
    "cycle_n3_w128",
    "cycle_master_n3_w128",
    "cycle_cycle_cycle_w128",
    
    "wide_port",
    "many_bundles",
    "many_modules",
    
    "tarjan",
    "invalid",
]

metric_names = [
    "num_connections",
    "num_modules",
    "num_clusters",
    "cluster_time",
    "map_cost",
    "quench",
    "map_time",
    "noc_in_bundles",
    "noc_out_bundles",
    "overutil_links",
    #"max_link_util",
    #"noc_bw_util",
]

#initialize web file
init_web(web_dir, version, metric_names)


#*******************************************************************************************
# Run/parse all the tests
#*******************************************************************************************
for test_name in test_names:
    
    curr_test_path = tests_dir+test_name+""+path_seperator+""+test_name+".xml"
    golden_path = tests_dir+test_name+""+path_seperator+"golden.txt"
    
    #vfile = tests_dir+test_name+""+path_seperator+""+test_name+".v"
    #os.system('rm '+vfile)

    if run:
        #------------------------------------------------------------------
        #run test
        #------------------------------------------------------------------
        print java_cmd+curr_test_path
        os.system(java_cmd+curr_test_path)
        
        #------------------------------------------------------------------
        #copy report
        #------------------------------------------------------------------
        if not os.path.isdir("archive/"+str(version)):
            os.mkdir("archive/"+str(version))
        shutil.copy2(curr_test_path+".rpt", "archive/"+str(version)+"/")
        os.remove(curr_test_path+".rpt")
    
    #------------------------------------------------------------------
    #parse report
    #------------------------------------------------------------------
    #path to current report file
    report_path = reports_dir+test_name+".xml.rpt"
    prev_report_path = prev_reports_dir+test_name+".xml.rpt"
    
    #first check for crash or non-existent report file
    crash = check_for_crash(report_path)
    
    #list of returned metrics
    metric_values=["-"]*len(metric_names)
    golden_values=["-"]*len(metric_names)
    previous_values=["-"]*len(metric_names)
    if not crash:
        metric_values = find_metrics(report_path, metric_names)
        golden_values = find_golden(golden_path, metric_names)
        previous_values = find_metrics(prev_report_path, metric_names)
    
    #------------------------------------------------------------------
    #update test page
    #------------------------------------------------------------------
    update_web(web_dir, version, test_name, crash, metric_values, golden_values, previous_values)
    
    
close_web(web_dir, version)
