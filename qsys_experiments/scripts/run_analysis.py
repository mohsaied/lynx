import os
import shutil
import time

    
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

java_cmd = "java -cp \""+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jcommon-1.0.23"+path_seperator+"jcommon-1.0.23.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jfreechart-1.0.19"+path_seperator+"lib"+path_seperator+"jfreechart-1.0.19.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jgraphx"+path_seperator+"jgraphx.jar\" lynx.main.Main --analysis"

curr_test="multimaster_n15_w128"

test_dir = path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"designs"+path_seperator+curr_test

trace_dir = "all_reports"
reports_dir = "parsed_reports"

    
if os.path.exists(reports_dir):
    shutil.rmtree(reports_dir)
os.mkdir(reports_dir)


#*******************************************************************************************
# Run/parse all the tests
#*******************************************************************************************
for i in range(1,50,1):
    
    test_name = "credits_"+str(i)+".txt"
    
    lynx_trace = trace_dir+path_seperator+test_name
    
    if not os.path.isfile(lynx_trace):
        continue;
    
    output_report = reports_dir+path_seperator+test_name

    #------------------------------------------------------------------
    #run test
    #------------------------------------------------------------------
    print java_cmd+" "+lynx_trace+" "+output_report+"\n"
    os.system(java_cmd+" "+lynx_trace+" "+output_report)
    
    