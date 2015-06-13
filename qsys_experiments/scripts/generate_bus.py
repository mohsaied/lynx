import os
import time
import sys
import shutil

import qsys_utils
from qsys_utils import *

import quartus_utils
from quartus_utils import *

import modelsim_utils
from modelsim_utils import *

import parse_utils
from parse_utils import *

#********************************************************************************************#
#				      PROGRAM START
#********************************************************************************************#

#parameters
num_master = int(sys.argv[1]) 
width = int(sys.argv[3])
num_pipeline = int(sys.argv[4])

if not os.path.exists('../'+project_name+'/'+project_name+'/synthesis/'+project_name+'.pow.rpt'):

	#--------------------------------------------------------------------------------------------#
	# Generate Qsys file then generate simulation outputs
	#--------------------------------------------------------------------------------------------#

	#change directory
	os.chdir("../"+project_name)

	#generate testbench files
	os.system("qsys-generate --testbench=STANDARD --testbench-simulation=VERILOG "+project_name+".qsys")

	#change directory back
	os.chdir("../scripts")

	#copy .qsys file to output directory
	shutil.copy("../"+project_name+"/"+project_name+".qsys",".")

	#--------------------------------------------------------------------------------------------#
	# Create Quartus project and compile it
	#--------------------------------------------------------------------------------------------#

	#create new project
	quartus_new_project(project_name,num_master)

	#change to project directory
	os.chdir('../'+project_name+'/'+project_name+'/synthesis')

	#create_project, run synthesis, p&r, sta, and assembler
	os.system("quartus_sh -t "+project_name+".tcl")
	os.system("quartus_map "+project_name)
	os.system("quartus_fit "+project_name)
	os.system("quartus_sta "+project_name+" --do_report_timing")
	os.system("quartus_asm "+project_name)

	#change to scripts directory
	os.chdir('../../../scripts')


	#--------------------------------------------------------------------------------------------#
	# Run modelsim simulation and capture toggle rates --> use in power simulation
	#--------------------------------------------------------------------------------------------#

	#prepare do file
	modelsim_do_file(project_name)

	#navigate to directory
	os.chdir('../'+project_name+'/'+project_name+'/testbench/mentor')

	#run modelsim
	os.system("vsim -c -do "+project_name+".do")

	#change directory to synthesis
	os.chdir('../../synthesis')

	#power analysis
	os.system("quartus_pow "+project_name+" --input_vcd=../testbench/mentor/"+project_name+".vcd --vcd_filter_glitches=on --output_saf="+project_name+".saf")

	#change to simulation directory
	os.chdir('../../../scripts')

	#take a copy of the map/fit/sta/pwr reports
	shutil.copy("../"+project_name+"/"+project_name+"/synthesis/"+project_name+".map.rpt", ".")
	shutil.copy("../"+project_name+"/"+project_name+"/synthesis/"+project_name+".fit.rpt", ".")
	shutil.copy("../"+project_name+"/"+project_name+"/synthesis/"+project_name+".sta.rpt", ".")
	shutil.copy("../"+project_name+"/"+project_name+"/synthesis/"+project_name+".pow.rpt", ".")
	shutil.copy("../"+project_name+"/"+project_name+"/synthesis/"+project_name+".saf", ".")

#--------------------------------------------------------------------------------------------#
# Parse area/delay/power and writeout to report files
#--------------------------------------------------------------------------------------------#

#overall metrics
alms  = parse_alms(project_name)
brams = parse_brams(project_name)
area  = float(alms)/10 + float(brams)*2.87
freq  = parse_freq(project_name)
pwr   = parse_power(project_name)
cpath = parse_cpath(project_name)

#formatted report line
report_line_formatted = str(num_master)+"\t"+str(num_slave)+"\t"+str(width)+"\t"+str(num_pipeline)+"\t"+str(diff_clock_master)+"\t"+str(diff_clock_slave)

#detailed area/pwr breakdown
parse_area_detailed(project_name,report_line_formatted,report_fname_area)
parse_pwr_detailed(project_name,report_line_formatted,report_fname_pwr,width)

print "\nArea = ", area, "LABs, Freq. = ", freq, "MHz, Power = ",pwr,"mW\n"," cpath = ",cpath

#write to report file
report_file = open(report_fname,'a')
print >>report_file, report_line_formatted+"\t"+str(alms)+"\t"+str(brams)+"\t"+str(area)+"\t"+str(freq)+"\t"+str(pwr)+"\t"+str(cpath)
report_file.close()

#--------------------------------------------------------------------------------------------#
# Done
#--------------------------------------------------------------------------------------------#
