import os
import time
import sys
import shutil
import random

#********************************************************************************************#
#				      PROGRAM START
#********************************************************************************************#

#parameters to test
num_modules = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]
widths      = [40,80,120,160,200,240,280,320,360,400,440,480,520,560,600]


num_modules = [6,7,8,9,10,11,12,13,14,15,16]
widths      = [120,240,360,480,600]



#create a report file, one for each parallel size
rpt_file = open('report.txt','w')
print>>rpt_file, 'num_modules\twidth\tfreq(MHz)'
rpt_file.close()

if not os.path.exists("experiments"):
    os.mkdir("experiments")

#for each configuration
for num_module in num_modules:
    for width in widths:
        
        project_name = "num_"+str(num_module)+"_width_"+str(width)
        project_path = 'experiments/'+project_name
        
        #if the sta report is not there
        if os.path.exists(project_path+'/output_files/noc_on_fpga.sta.rpt'):
            print "Quartus already run for "+project_name+", skipping this run.."
        else:
        
            #remove anything that was there before
            if os.path.exists(project_path):
                print "Deleting incomplete existing directory"
                shutil.rmtree(project_path)
            
            #copy the golden project into a new one
            print "Making a fresh copy"
            shutil.copytree("noc_on_fpga", project_path)
            
            #clean old reports
            print "Cleaning old reports"
            shutil.rmtree(project_path+"/output_files")
            
            #copy top file
            print "modifying top file"
            shutil.copy(project_path+"/src/noc_on_fpga.sv",project_path+"/src/temp.sv")
            
            #vary the parameters
            top_file = open(project_path+"/src/temp.sv",'r')
            new_top_file = open(project_path+"/src/noc_on_fpga.sv",'w')
            for line in top_file:
                if "localparam MOD_WIDTH" in line:
                    line = "localparam MOD_WIDTH = "+str(width)+";\n"
                elif "localparam NUM_MODS" in line:
                    line = "localparam NUM_MODS = "+str(num_module)+";\n"
                print >>new_top_file,line,
            top_file.close()
            new_top_file.close()
            
            #run it!
            os.chdir(project_path)
            os.system("quartus_map noc_on_fpga")
            os.system("quartus_fit noc_on_fpga ")
            os.system("quartus_sta noc_on_fpga --do_report_timing")
            os.chdir("../../")
        
            #parse frequency
            sta_file = open(project_path+'/output_files/noc_on_fpga.sta.rpt','r')
            
            flag = False	
            freq = -1
            for line in sta_file:
                if '; Slow 900mV 85C Model Fmax Summary' in line:
                    flag = True
                if flag and 'clk' in line:
                    print line
                    part_list = line.split()
                    freq = float(part_list[1].strip())
                    break
            
            sta_file.close()
            
            #writeout to report file
            rpt_file = open('report.txt','a')
            print>>rpt_file, str(num_master)+'\t'+str(width)+'\t'+str(freq)
            rpt_file.close()
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		