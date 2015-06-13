import os.path
import os
import time
import sys
import math


#------------------------------------#	
#create new project 
#------------------------------------#	
def modelsim_do_file(project_name):
	
	#open file
	do_file = open('../'+project_name+'/'+project_name+'/testbench/mentor/'+project_name+'.do','w')
	
	#writeout preamble
	print >>do_file, \
	"source msim_setup.tcl\n\
ld_debug\n\
add wave *\n\
view structure\n\
view signals\n\
run 5000ns\n\
set vcd_filename \""+project_name+".vcd\"\n\
set hierarchy \""+project_name+"\"\n\
vcd file \"$vcd_filename\"\n\
vcd on\n\
vcd add -r /"+project_name+"_tb/"+project_name+"_inst/*\n\
run 50000ns\n\
stop\n\
quit"
	
	#close file
	do_file.close()
	
	#modify SDC file to 100 MHz
	sdc_file = open('../'+project_name+'/'+project_name+'/synthesis/'+project_name+'.sdc','w')
	print >>sdc_file, "create_clock -period 10 -name clk_clk clk_clk\n\
create_clock -period 10 -name clk_0_clk clk_0_clk\n\
derive_clock_uncertainty"
	sdc_file.close()
