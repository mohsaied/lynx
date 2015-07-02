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
num_masters   = [5,8,11,14]
widths        = [128,256,512]
num_pipelines = [4]
multiclocks   = [0]
seeds         = [1,5,8,17,43]

#--------------------------------------------------------------------------------------------#

#start timer
initial_time = time.time()

#initialize variables
num_systems = 0

#create new report file
rpt_fname = "bus_report.txt"
rpt_file = open(rpt_fname,'w')
print>>rpt_file, 'seed\tnum_modules\twidth\tfreq(MHz)'
rpt_file.close()


#############################################################################
#run stuff
#############################################################################

for num_master in num_masters:
    for width in widths:
        for num_pipeline in num_pipelines:
            for multiclock in multiclocks:
                for seed in seeds:
                    generate_bus(num_master,width,num_pipeline,multiclock,seed,rpt_fname)
                    num_systems = num_systems + 1
                
                
#end time:
final_time = time.time()

print "Generated "+str(num_systems)+" systems in "+str((final_time-initial_time)/60)+" minutes"
