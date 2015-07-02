import os
import time
import sys

import vary_credits_prio
from vary_credits_prio import *

#********************************************************************************************#
#                             PROGRAM START
#********************************************************************************************#

#Golden parameter set
num_masters      = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
baseline_credits = [9, 6, 4, 4, 3, 2, 2, 2,  2,  1,  1,  1,  1,  1]

#--------------------------------------------------------------------------------------------#

#start timer
initial_time = time.time()

#initialize variables
num_systems = 0

#############################################################################
#run stuff
#############################################################################

for i in range(len(num_masters)):
    vary_credits_prio("multimaster_n"+str(num_masters[i])+"_w128",baseline_credits[i])
    num_systems = num_systems + 10
                
                
#end time:
final_time = time.time()

print "Generated "+str(num_systems)+" systems in "+str(final_time-initial_time)+" seconds"

