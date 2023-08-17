package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Scene;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import java.io.FileWriter;
import java.io.IOException;
import soot.SootMethod;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;



import java.io.FileReader;
import org.xmlpull.v1.XmlPullParserException;

import reunify.ReactModulesAndViewPreAnalysisHandler;
import reunify.ReactUtil;


public class DetectNumberEdgesWithReactUpdate {
    static String targetFile = "/your/dir/to/reactNativeOutput/malware/RNAppsFlowDroidRN.csv";
    static String stasticOutFile = "/your/dir/to/reactNativeOutput/malware/RNAppsFlowDroidRNCopy.csv";


    public static void main(String[] args) {
        String appFolder = "/your/dir/to/malware/react_native/VirusShare_2022_1-3_RN";
        String xmlOutputFolder = "/your/dir/to/react-native/reactNativeOutput/malware/flowdroidRN";

    
        String apkPath;
        String xmlFile;
        
        String androidJars = "/your/dir/to/tools/android-platforms";
        String sourcesAndSinks = "/your/dir/to/react-native/reactNativeInput/flowdroid/SourcesAndSinks.txt";

        List<String> sha256Array = readFromCSV(targetFile);
        sha256Array.remove(0);


        int count = 0;

        for (Object o : sha256Array) {
            
            count++;
            String sha256 = o.toString(); 
            

            ReactUtil.clearReactUtil();

            String[] potentialSHA256 = readOneFailed(stasticOutFile);

            sha256 = potentialSHA256[0];
            

            System.out.println("+++++++++++++++++++++++++++++" +"Currently, working on: " + count + "+++++++++++++++"+ sha256 + "+++++++++++++++++++++++++++++");
            long startTime = System.currentTimeMillis();
            String[] data = new String[6];
            data[0] = sha256;

            apkPath = appFolder + "/" + sha256 + ".apk";
            xmlFile = xmlOutputFolder + "/" + sha256 + ".xml";

            SetupApplication setupApplication = new SetupApplication(androidJars, apkPath);
            InfoflowAndroidConfiguration config = setupApplication.getConfig();

            
            try {

                config.setMergeDexFiles(true);
                config.getAnalysisFileConfig().setOutputFile(xmlFile);
                config.setDataFlowTimeout(600);
                config.getCallbackConfig().setCallbackAnalysisTimeout(600);
                config.getPathConfiguration().setPathReconstructionTimeout(600);
                setupApplication.addPreprocessor(new ReactModulesAndViewPreAnalysisHandler(setupApplication));

                InfoflowResults infoflowResults = setupApplication.runInfoflow(sourcesAndSinks);

                long endTime = System.currentTimeMillis(); 
                long elapsedTime = endTime - startTime; 

                CallGraph callGraph = Scene.v().getCallGraph();

                data[1] = "true";
                data[2] = String.valueOf(callGraph.size());
                data[3] = String.valueOf(getAllReachableMethods(setupApplication.getDummyMainMethod()).size());                
                if(infoflowResults.getResultSet() != null){
                    data[4] = String.valueOf(infoflowResults.getResultSet().size());
                }else{
                    data[4] = "0";
                }
                data[5] = String.valueOf(elapsedTime);

            } catch (XmlPullParserException | RuntimeException | IOException | OutOfMemoryError e ) {
                e.printStackTrace();
                long endTime = System.currentTimeMillis(); 
                long elapsedTime = endTime - startTime; 

                data[1] = "false";
                data[2] = "unknown";
                data[3] = "unknown";
                data[4] = "unknown";
                data[5] = String.valueOf(elapsedTime);
            }
                        
            updateCSVRow(data, potentialSHA256, stasticOutFile);
        }

    }

    public static List<String> readFromCSV(String file){
        String line = "";
        String csvDelimiter = ",";
        List<String> sha256List = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvDelimiter);
                if(!data[1].equals("false")){
                    sha256List.add(data[0]);
                }
            }
            
        } catch (IOException e) {
            writeAndConstructCSV(file);
        }


        return sha256List;
    }


    public static String[] readOneFailed(String file){
        String line = "";
        String csvDelimiter = ",";
        String[] targetedSha256 = {"null"};
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvDelimiter);
                
                if(data[1].equals("false")){                
                    targetedSha256 = data;
                    break;
                }
            }
            
        } catch (IOException e) {
            writeAndConstructCSV(file);
        }
        

        return targetedSha256;
    }

    

    public static void writeAndConstructCSV(String file){
        try {
            FileWriter writer = new FileWriter(file);
            
            writer.write("sha256,isSuccess,numOfEdges,numOfMethods,numOfLeaks,elapsedTime");
            writer.close();
            System.out.println("Data written to CSV file successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void addCSVRow(String[] rowData, String outFile){
        String csvFile = outFile;
        FileWriter writer = null;
        try {
            writer = new FileWriter(csvFile, true);
            writer.write("\n"); 
            writer.write(rowData[0]+","+rowData[1]+","+rowData[2]+","+rowData[3]+","+rowData[4]+","+rowData[5]); // write the new row

            System.out.println("New row added to CSV file");
        } catch (IOException e) {
            System.out.println("Error adding new row to CSV file: " + e.getMessage());
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                System.out.println("Error closing CSV file: " + e.getMessage());
            }
        }
    }


    public static void updateCSVRow(String[] rowData, String[] oldRowData, String fileUpdate){
        List<String[]> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileUpdate))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                lines.add(fields);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }        

        File file = new File(fileUpdate);

        file.delete();
        
        try (
            FileWriter writer = new FileWriter(fileUpdate, true)) { 

                for(String[] line:lines){{
                    if (line[0].equals(oldRowData[0])) { 
                        writer.write(String.join(",", rowData) + "\n"); 
                    } else {
                        writer.write(String.join(",", line) + "\n");
                    }
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }




    public static Map<SootMethod, SootMethod> getAllReachableMethods(SootMethod initialMethod){
        CallGraph callgraph = Scene.v().getCallGraph();
        List<SootMethod> queue = new ArrayList<>();
        queue.add(initialMethod);
        Map<SootMethod, SootMethod> parentMap = new HashMap<>();
        parentMap.put(initialMethod, null);
        for(int i=0; i< queue.size(); i++){
            SootMethod method = queue.get(i);
            for (Iterator<Edge> it = callgraph.edgesOutOf(method); it.hasNext(); ) {
                Edge edge = it.next();
                SootMethod childMethod = edge.tgt();
                if(parentMap.containsKey(childMethod))
                    continue;
                parentMap.put(childMethod, method);
                queue.add(childMethod);
            }
        }
        
        return parentMap;
    }
}
