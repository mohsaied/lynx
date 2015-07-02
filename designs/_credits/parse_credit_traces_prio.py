import os
import time
import sys

####################################################################################################
############## PARSE PROGRAM START ##################################################################
####################################################################################################

#Golden parameter set
num_masters      = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
baseline_credits = [9, 6, 4, 4, 3, 2, 2, 2,  2,  1,  1,  1,  1,  1]

platform = "windows"
#platform = "linux"

path_start = "D:"
path_seperator = "\\"
if platform == "linux":
    path_start = "/home/mohamed"
    path_seperator = "/"

java_cmd = "java -cp \""+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jcommon-1.0.23"+path_seperator+"jcommon-1.0.23.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jfreechart-1.0.19"+path_seperator+"lib"+path_seperator+"jfreechart-1.0.19.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jgraphx"+path_seperator+"jgraphx.jar\" lynx.main.Main --analysis"

num_test = 0

for num_master in num_masters:

    curr_test = "multimaster_n"+str(num_master)+"_w128"

    test_dir = path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"designs"+path_seperator+curr_test

    trace_dir = test_dir+path_seperator+"sim"+path_seperator+"credits_prio"
    reports_dir = test_dir+path_seperator+"sim"+path_seperator+"credits_prio_rpt"

    if not os.path.isdir(reports_dir):
        os.mkdir(reports_dir)
    
    base_cred = baseline_credits[num_test]
    num_test = num_test+1

    #*******************************************************************************************
    # Run/parse all the tests
    #*******************************************************************************************
    for i in range(1,10,1):
        
        test_name = "credits_"+str(i*base_cred)+".txt"
        
        lynx_trace = trace_dir+path_seperator+test_name
        
        if not os.path.isfile(lynx_trace):
            continue;
        
        output_report = reports_dir+path_seperator+test_name

        #------------------------------------------------------------------
        #run test
        #------------------------------------------------------------------
        print java_cmd+" "+lynx_trace+" "+output_report+"\n"
        os.system(java_cmd+" "+lynx_trace+" "+output_report)
        
        