import os
import shutil
import time

####################################################################################################
############## MAIN PROGRAM START ##################################################################
####################################################################################################

test_names = [
    "chain_n4_w128",
    "chain_n16_w128",
    "chain_n20_w128",
    
    "broadcast_n3_w128",
    "broadcast_n9_w128",
    "broadcast_n16_w128",
    
    "converge_n3_w128",
    "converge_n9_w128",
    "converge_n18_w128",
    
    "multimaster_n3_w128",
    "multimaster_n9_w128",
    "multimaster_n17_w128",
    
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

platform = "windows"
platform = "linux"

path_start = "D:"
path_seperator = "\\"
if platform == "linux":
    path_start = "/home/mohamed"
    path_seperator = "/"


tests_dir = path_start+path_seperator+"Dropbox"+path_seperator+"PhD"+path_seperator+"Software"+path_seperator+"noclynx"+path_seperator+"designs"+path_seperator+""

#*******************************************************************************************
# Run/parse all the tests
#*******************************************************************************************

for test_name in test_names:
    
    curr_test = tests_dir+test_name+path_seperator+"sim"+path_seperator+"quick_script"
    
    #------------------------------------------------------------------
    #run sim
    #------------------------------------------------------------------
    print curr_test
    os.system(curr_test)
        
