import os
import time
import sys

import vary_credits
from vary_credits import *

#********************************************************************************************#
#                             PROGRAM START
#********************************************************************************************#

#Golden parameter set
num_masters   = [2,3,4,5,6,7,8,9,10,11,12,13,14,15]


#--------------------------------------------------------------------------------------------#

#start timer
initial_time = time.time()

#initialize variables
num_systems = 0

#############################################################################
#run stuff
#############################################################################

for num_master in num_masters:
    vary_credits("multimaster_n"+str(num_master)+"_w128")
    num_systems = num_systems + 10
                
                
#end time:
final_time = time.time()

print "Generated "+str(num_systems)+" systems in "+str(final_time-initial_time)+" seconds"

