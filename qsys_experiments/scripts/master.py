import os
import time
import sys

import generate_bus
from generate_bus import *

#********************************************************************************************#
#                             PROGRAM START
#********************************************************************************************#

#Golden parameter set
num_masters   = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]
widths        = [16,32,64,128,256,512,1024]
num_pipelines = [0,1,2,3,4]
multiclocks   = [0,1]

#parameters being tested
num_masters   = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]
widths        = [32]
num_pipelines = [4]
multiclocks   = [1]

#--------------------------------------------------------------------------------------------#

#start timer
initial_time = time.time()

#initialize variables
num_systems = 0

#clear old reports
if not os.path.exists("all_reports"):
    os.mkdir("all_reports")

#############################################################################
#run stuff
#############################################################################

for num_master in num_masters:
    for width in widths:
        for num_pipeline in num_pipelines:
            for multiclock in multiclocks:
                generate_bus(num_master,width,num_pipeline,multiclock)
                num_systems = num_systems + 1
                
                
#end time:
final_time = time.time()

print "Generated "+str(num_systems)+" systems in "+str(final_time-initial_time)+" seconds"

####################################################################################################
############## PARSE PROGRAM START ##################################################################
####################################################################################################

platform = "windows"
#platform = "linux"

path_start = "D:"
path_seperator = "\\"
if platform == "linux":
    path_start = "/home/mohamed"
    path_seperator = "/"

java_cmd = "java -cp \""+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jcommon-1.0.23"+path_seperator+"jcommon-1.0.23.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jfreechart"+path_seperator+"jfreechart-1.0.19"+path_seperator+"lib"+path_seperator+"jfreechart-1.0.19.jar;"+path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"jgraphx"+path_seperator+"jgraphx.jar\" lynx.main.Main --analysis"

trace_dir = "all_reports"
reports_dir = "parsed_reports"
    
if not os.path.exists(reports_dir):
    os.mkdir(reports_dir)

#*******************************************************************************************
# Run/parse all the tests
#*******************************************************************************************
for num_master in num_masters:
    for width in widths:
        for num_pipeline in num_pipelines:
            for multiclock in multiclocks:
    
                test_name = 'm'+str(num_master)+'_w'+str(width)+'_p'+str(num_pipeline)+"_c"+str(multiclock)+".txt";
                
                lynx_trace = trace_dir+path_seperator+test_name
                
                if not os.path.isfile(lynx_trace):
                    continue;
                
                output_report = reports_dir+path_seperator+test_name

                #------------------------------------------------------------------
                #run analysis
                #------------------------------------------------------------------
                print java_cmd+" "+lynx_trace+" "+output_report+"\n"
                os.system(java_cmd+" "+lynx_trace+" "+output_report)
    
    