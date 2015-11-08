package lynx.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.MyEnums.Direction;
import lynx.main.ReportData;
import lynx.nocmapping.Mapping;
import lynx.main.DesignData;

/**
 * A top-level design that is the result of parsing the XML file. It contains all the modules and connections. 
 * 
 * @author Mohamed
 *
 */
public final class Design extends Module {

	private static final Logger log = Logger.getLogger(Design.class.getName());

	private Noc noc;

	private Map<String, DesignModule> modules;
	private Map<String, Integer> moduleIndices;

	private DesignModule haltModule;

	private List<Connection> allConnections;

	private List<Translator> translators;

	private List<Wrapper> wrappers;

	// output of NoC clustering
	private List<Set<String>> clusters;

	// outputs of NoC mapping
	private List<ArrayList<Mapping>> mappings;
	private List<Double> debugAnnealCost;
	private List<Double> debugAnnealTemp;

	public Design() {
		this(null);
	}

	public Design(String name) {
		super(name, name + "_inst");
		this.modules = new LinkedHashMap<String, DesignModule>();
		this.moduleIndices = new HashMap<String, Integer>();
		this.allConnections = new ArrayList<Connection>();
		this.translators = new ArrayList<Translator>();
		this.wrappers = new ArrayList<Wrapper>();
		this.mappings = null;
		this.clusters = null;
		this.noc = null;
		this.haltModule = null;
		this.setDebugAnnealCost(new ArrayList<Double>());
		log.info("Creating new design: " + name);
	}

	public Design(String Name, Noc noc) {
		this(Name);
		this.noc = noc;
	}

	public Noc getNoc() {
		return noc;
	}

	public void setNoc(Noc noc) {
		this.noc = noc;
	}

	public final Map<String, DesignModule> getDesignModules() {
		return modules;
	}

	public final void addModule(DesignModule currModule) {
		this.modules.put(currModule.getName(), currModule);
		// hash module name with an index
		this.moduleIndices.put(currModule.getName(), this.moduleIndices.size());
	}

	public final int getNumModules() {
		return modules.size();
	}

	public final int getNumDesignModules() {
		return moduleIndices.size();
	}

	public final List<Module> getAllModules() {
		List<Module> allModules = new ArrayList<Module>();

		allModules.addAll(modules.values());

		if (!translators.isEmpty())
			allModules.addAll(translators);

		if (!wrappers.isEmpty())
			allModules.addAll(wrappers);

		if (noc == null)
			allModules.add(DesignData.getInstance().getNoc());
		else
			allModules.add(noc);

		if (haltModule != null)
			allModules.add(haltModule);

		return allModules;
	}

	public final Module getModuleByName(String modName) {
		return this.modules.get(modName);
	}

	public List<Translator> getTranslators() {
		return translators;
	}

	public void addTranslator(Translator translator) {
		this.translators.add(translator);
	}

	@Override
	public void addPort(Port wire) {
		// if we dont have a port with the same name
		// create a new one and add to it a wire
		if (!this.getPorts().containsKey(wire.getGlobalPortName())) {
			Port topPort = new Port(wire, this);
			topPort.addWire(wire);
			super.addPort(topPort);
		} else {
			// if we do have a port, we'll just add another wire to it
			Port topPort = this.getPortByName(wire.getGlobalPortName());
			topPort.addWire(wire);
		}
	}

	@Override
	public String toString() {
		String s = "Design: " + name + "\n\n";
		for (Port por : this.ports.values())
			s += por + "\n";
		s += "\n";
		for (Module mod : this.modules.values())
			s += mod + "\n";
		return s;
	}

	private DesignModule getModuleByIndex(int modIndex) {
		for (String modName : this.moduleIndices.keySet()) {
			if (this.moduleIndices.get(modName) == modIndex)
				return (DesignModule) getModuleByName(modName);
		}
		return null;
	}

	public boolean[][] getAdjacencyMatrix() {
		int numModules = modules.size();
		boolean[][] matrix = new boolean[numModules][numModules];
		// init matrix
		for (int i = 0; i < numModules; i++)
			for (int j = 0; j < numModules; j++)
				matrix[i][j] = false;

		for (String modName : this.moduleIndices.keySet()) {

			// current module
			DesignModule currModule = (DesignModule) getModuleByName(modName);
			// current module index
			int i = this.moduleIndices.get(modName);

			// find all modules connected to module i
			List<String> conMods = currModule.getConnectedModuleNames(Direction.OUTPUT);
			for (String conModName : conMods) {
				int j = this.moduleIndices.get(conModName);
				matrix[i][j] = true;
			}
		}
		return matrix;
	}

	public int getModuleInDegree(int modIndex) {
		DesignModule currModule = this.getModuleByIndex(modIndex);
		return currModule.getConnectedModuleNames(Direction.INPUT).size();
	}

	public int getModuleOutDegree(int modIndex) {
		DesignModule currModule = this.getModuleByIndex(modIndex);
		return currModule.getConnectedModuleNames(Direction.OUTPUT).size();
	}

	public int getModuleIndex(String fromModuleName) {
		return this.moduleIndices.get(fromModuleName);
	}

	public List<Connection> getConnections() {
		return allConnections;
	}

	public void update() {

		// sort moduleindices by radix
		// (a module with a higher radix should have a lower module index)

		Map<String, Integer> radices = new HashMap<String, Integer>();

		// find the # connections for each module
		for (String modString : moduleIndices.keySet()) {

			DesignModule m = (DesignModule) getModuleByName(modString);

			int modRadixOut = m.getConnectedModuleNames(Direction.OUTPUT).size();
			int modRadixIn = m.getConnectedModuleNames(Direction.INPUT).size();
			int maxRadix = Math.max(modRadixIn, modRadixOut);

			radices.put(modString, maxRadix);
		}

		ArrayList<String> sortedModules = new ArrayList<String>();
		for (String modName : radices.keySet()) {
			sortedModules.add(modName);
		}

		// sort by radix
		for (int i = 0; i < sortedModules.size(); i++) {
			for (int j = i; j < sortedModules.size(); j++) {

				// loop over sortedModules and do insertion sort into new array
				int currRadix = radices.get(sortedModules.get(i));
				int otherRadix = radices.get(sortedModules.get(j));

				if (otherRadix > currRadix) {
					String temp = sortedModules.get(j);
					sortedModules.set(j, sortedModules.get(i));
					sortedModules.set(i, temp);
				}
			}
		}

		// insert back into moduleIndices sorted
		for (String modName : moduleIndices.keySet()) {
			int pos = 0;
			for (int j = 0; j < sortedModules.size(); j++)
				if (sortedModules.get(j).equals(modName)) {
					pos = j;
					break;
				}
			moduleIndices.put(modName, pos);
		}

		allConnections.clear();

		for (DesignModule mod : this.modules.values()) {

			// find all modules connected to module i
			Map<String, Bundle> conBuns = mod.getBundles();
			for (Bundle bun : conBuns.values()) {
				if (bun.getDirection() == Direction.OUTPUT) {
					for (Bundle toBun : bun.getConnections()) {
						Connection con = new Connection(bun, toBun, this);
						allConnections.add(con);
					}
				}
			}
		}
	}

	public List<ArrayList<Mapping>> getMappings() {
		return mappings;
	}

	public void setMappings(List<ArrayList<Mapping>> equivSimMappings) {
		mappings = equivSimMappings;

		// find first mapping
		Mapping bestMapping = equivSimMappings.get(0).get(0);
		int numNocInBundlesUsed = bestMapping.getNumNoCBundlesIn();
		int numNocOutBundlesUsed = bestMapping.getNumNoCBundlesOut();
		ReportData.getInstance().writeToRpt("noc_in_bundles = " + numNocInBundlesUsed);
		ReportData.getInstance().writeToRpt("noc_out_bundles = " + numNocOutBundlesUsed);
	}

	public void setSingleMapping(Mapping currMapping) {
		List<ArrayList<Mapping>> equivSimMappings = new ArrayList<ArrayList<Mapping>>();
		ArrayList<Mapping> mappingList = new ArrayList<Mapping>();
		mappingList.add(currMapping);
		equivSimMappings.add(mappingList);

		this.setMappings(equivSimMappings);
	}

	public List<Double> getDebugAnnealCost() {
		return debugAnnealCost;
	}

	public void setDebugAnnealCost(List<Double> debugAnnealCost) {
		this.debugAnnealCost = debugAnnealCost;
	}

	public List<Double> getDebugAnnealTemp() {
		return debugAnnealTemp;
	}

	public void setDebugAnnealTemp(List<Double> debugAnnealTemp) {
		this.debugAnnealTemp = debugAnnealTemp;
	}

	public List<Set<String>> getClusters() {
		return clusters;
	}

	public void setClusters(List<Set<String>> clusters) {
		this.clusters = clusters;
	}

	public int getNumInBundles() {
		int numBuns = 0;
		for (DesignModule mod : getDesignModules().values()) {
			for (Bundle bun : mod.getBundles().values()) {
				if (bun.getDirection() == Direction.INPUT)
					numBuns++;
			}
		}
		return numBuns;
	}

	public int getNumOutBundles() {
		int numBuns = 0;
		for (DesignModule mod : getDesignModules().values()) {
			for (Bundle bun : mod.getBundles().values()) {
				if (bun.getDirection() == Direction.OUTPUT)
					numBuns++;
			}
		}
		return numBuns;
	}

	public List<Bundle> getAllBundles() {
		List<Bundle> bundleList = new ArrayList<Bundle>();

		for (DesignModule mod : getDesignModules().values()) {
			for (Bundle bun : mod.getBundles().values()) {
				bundleList.add(bun);
			}
		}

		return bundleList;
	}

	@Override
	public Design clone() {

		Design cloneDesign = new Design(this.getName());

		return cloneDesign;
	}

	public DesignModule getHaltModule() {
		return haltModule;
	}

	public void setHaltModule(DesignModule haltModule) {
		this.haltModule = haltModule;
	}

	public List<Wrapper> getWrappers() {
		return wrappers;
	}

	public void addWrapper(Wrapper wrapper) {
		this.wrappers.add(wrapper);
	}

}
