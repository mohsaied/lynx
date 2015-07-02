import os
import time
import sys
import shutil
import math

#********************************************************************************************#
#                     PROGRAM START
#********************************************************************************************#

def generate_bus(num_master,width,num_pipeline,multiclock,seed,rpt_fname):

    project_name = 'm'+str(num_master)+'_w'+str(width)+'_p'+str(num_pipeline)+"_c"+str(multiclock)+"_sd"+str(seed);
    project_path = "../physical_experiments/"+project_name

    addr_width = 32-math.log(float(float(width)/8),2)
    
    if os.path.exists('../physical_experiments/'+project_name+'/synthesis/output_files/'+project_name+'.sta.rpt'):
        print "Simulation already run for "+project_name+", skipping this run.."
    else:

        #--------------------------------------------------------------------------------------------#
        # Generate Qsys file then generate simulation outputs
        #--------------------------------------------------------------------------------------------#

        #create project directory and delete any old one
        if os.path.exists(project_path):
            shutil.rmtree(project_path)
        os.mkdir(project_path)
            
        #copy base project from origs to experiments and rename it
        if multiclock == 0:
            shutil.copy("../physical_origs/m"+str(num_master)+".qsys", project_path+"/"+project_name+".qsys")
        else:
            shutil.copy("../physical_origs/m"+str(num_master)+"_clk.qsys", project_path+"/"+project_name+".qsys")
            
        shutil.copy("../physical_origs/qsys_master_hw.tcl",project_path)
        shutil.copy("../physical_origs/qsys_slave_hw.tcl",project_path)
        
        #change directory
        os.chdir(project_path)
        
        
        
        print "actual data width used is "+str(width+32)+", and addr_width = "+str(addr_width)
        
        shutil.copy(project_name+".qsys" ,"temp.qsys")
        addr_found = False
        orig_qsys = open('temp.qsys','r')
        new_qsys = open(project_name+".qsys",'w')
        for line in orig_qsys:
            if "<parameter name=\"WIDTH\"" in line:
                line = "  <parameter name=\"WIDTH\" value=\""+str(width)+"\" />\n"
            if "<parameter name=\"ADDR_WIDTH\"" in line and not addr_found:
                addr_found = True
                line = "  <parameter name=\"ADDR_WIDTH\" value=\""+str(addr_width)+"\" />\n"
            print >>new_qsys, line, 
        orig_qsys.close()
        new_qsys.close()
        os.remove("temp.qsys")
        
        #generate testbench files
        os.system("ip-generate --search-path=\".,$\" --project-directory=. --output-directory=synthesis --file-set=QUARTUS_SYNTH --report-file=qip:synthesis\\"+project_name+".qip --system-info=DEVICE_FAMILY=\"Stratix V\" --system-info=DEVICE=5SGSED8K1F40C2 --system-info=DEVICE_SPEEDGRADE=2_H1 --component-file="+project_name+".qsys --language=VERILOG")

        #--------------------------------------------------------------------------------------------#
        # Copy template quartus project
        #--------------------------------------------------------------------------------------------#
        
        #dive into sim directory
        os.chdir("synthesis")
        
        #make a temp copy of top
        shutil.copy("../../../physical_origs/quartus.qpf", project_name+".qpf")
        shutil.copy("../../../physical_origs/quartus.qsf", project_name+".qsf")
        shutil.copy("../../../physical_origs/quartus.sdc", "quartus.sdc")
        
        shutil.copy(project_name+".qpf" ,"temp.qpf")
        shutil.copy(project_name+".qsf" ,"temp.qsf")
        
        #edit qpf with the right name
        orig_qpf = open('temp.qpf','r')
        new_qpf = open(project_name+".qpf",'w')
        for line in orig_qpf:
            if "REPLACE_THIS_PLEASE" in line:
                line = "PROJECT_REVISION = \""+project_name+"\"\n"
            print >>new_qpf, line, 
        orig_qpf.close()
        new_qpf.close()
        os.remove("temp.qpf")
        
        #edit qsf with the right name
        orig_qsf = open('temp.qsf','r')
        new_qsf = open(project_name+".qsf",'w')
        first_one = True
        second_one = False
        for line in orig_qsf:
            if "REPLACE_THIS_PLEASE" in line and first_one:
                line = "set_global_assignment -name TOP_LEVEL_ENTITY "+project_name+"\n"
                first_one = False
            if "REPLACE_THIS_PLEASE" in line and not first_one and not second_one:
                line = "set_global_assignment -name QIP_FILE "+project_name+".qip\n"
            print >>new_qsf, line, 
        orig_qsf.close()
        new_qsf.close()
        os.remove("temp.qsf")
        
        
        #--------------------------------------------------------------------------------------------#
        # run quartus
        #--------------------------------------------------------------------------------------------#
        
        os.system("quartus_map "+project_name)
        os.system("quartus_fit --seed="+str(seed)+" "+project_name)
        os.system("quartus_sta "+project_name+" --do_report_timing")
        
        #change directory back
        os.chdir("../../../physical_scripts")
    
    #--------------------------------------------------------------------------------------------#
    # Parse
    #--------------------------------------------------------------------------------------------#

    #parse frequency
    sta_file = open(project_path+'/synthesis/output_files/'+project_name+'.sta.rpt','r')
    
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
    
    rpt_file = open(rpt_fname,'a')
    print>>rpt_file, str(seed)+"\t"+str(num_master+1)+"\t"+str(width+addr_width)+"\t"+str(freq)
    rpt_file.close()
    
    #--------------------------------------------------------------------------------------------#
    # Done
    #--------------------------------------------------------------------------------------------#
