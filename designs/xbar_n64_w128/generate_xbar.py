

#xbar radix
radix = 64

#create a new webfile
outfile = open("xbar_n"+str(radix)+"_w128.xml",'w')
    
#write the file preamble
print >>outfile, "<design name=\"xbar\">\n"

#srcs
for i in range(1,radix+1):
    print >>outfile, "  <module type=\"sender\" name=\"src"+str(i)+"\">\n\
	<port direction=\"input\" width=\"1\" name=\"clk\" type=\"clk\" global=\"clk\"/>\n\
	<port direction=\"input\" width=\"1\" name=\"rst\" type=\"rst\" global=\"rst\"/>\n\
	<bundle name=\"outbun\">\n\
		<port direction=\"output\" width=\"128\" name=\"o_y\" type=\"data\"/>\n\
		<port direction=\"output\" width=\"1\" name=\"o_valid_out\" type=\"valid\"/>\n\
		<port direction=\"output\" width=\"4\" name=\"o_dest_out\" type=\"dst\"/>\n\
		<port direction=\"input\" width=\"1\" name=\"o_ready_in\" type=\"ready\"/>\n\
	</bundle>	\n\
  </module>\n"


#dsts
for i in range(1,radix+1):
    print >>outfile, "<module type=\"receiver\" name=\"dst"+str(i)+"\">\n\
	<port direction=\"input\" width=\"1\" name=\"clk\" type=\"clk\" global=\"clk\"/>\n\
	<port direction=\"input\" width=\"1\" name=\"rst\" type=\"rst\" global=\"rst\"/>\n\
	<bundle name=\"inbun\">\n\
		<port direction=\"input\" width=\"128\" name=\"i_x\" type=\"data\"/>\n\
		<port direction=\"input\" width=\"1\" name=\"i_valid_in\" type=\"valid\"/>\n\
		<port direction=\"output\" width=\"1\" name=\"i_ready_out\" type=\"ready\"/>\n\
	</bundle>\n\
  </module>\n"

#connections
for i in range(1,radix+1):
    for j in range(1,radix+1):
        print >>outfile, "<connection start=\"src"+str(j)+".outbun\" end=\"dst"+str(i)+".inbun\"/>"
    print >> outfile, ""


  
#write the file postamble
print >>outfile, "</design>\n"
  
outfile.close()