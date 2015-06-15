import os
import shutil
import time

####################################################################################################
############## MAIN PROGRAM START ##################################################################
####################################################################################################

def vary_credits(project_name):
    
    #dive into project sim directory
    os.chdir("../"+project_name+"/sim/")
    
    credits_range = range(1,10,1)
    tb_fname = "tb_"+project_name+".sv"
    
    reports_dir = "credits"

    if not os.path.isdir(reports_dir):
        os.mkdir(reports_dir)

    for num_credits in credits_range:
        print "working on num_credits = "+str(num_credits)
        
        #------------------------------------------------------------
        # replace the number of credits in the tb file
        #------------------------------------------------------------
        
        #replicate testbench into temp.sv
        shutil.copy(tb_fname, "temp.v")
        
        #open old and new tb files
        tb_orig  = open("temp.v",'r')
        tb_new  = open(tb_fname,'w')
        
        for line in tb_orig:
            if "NUM_CREDITS" in line:
                line = "    .NUM_CREDITS("+str(num_credits)+")\n"
            print>>tb_new, line,
        
        tb_orig.close()
        tb_new.close()
        
        #------------------------------------------------------------
        # run quickscript simulation
        #------------------------------------------------------------
        
        os.system("chmod u+x quick_script")
        os.system("./quick_script")
        
        #------------------------------------------------------------
        # copy the report 
        #------------------------------------------------------------
        
        shutil.copyfile("reports/lynx_trace.txt",reports_dir+"/credits_"+str(num_credits)+".txt")
        
    os.chdir("../../_credits")
        