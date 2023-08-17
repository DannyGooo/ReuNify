package experiments;

import java.util.ArrayList;
import java.util.List;

import java.io.FileWriter;
import java.io.IOException;

import experiments.temPreAnalysisHandler.ReactJSModulesCount;
import reunify.ReactUtil;
import reunify.SootConfigForAndroidRN;

import java.io.BufferedReader;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.results.InfoflowResults;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParserException;


public class DetectNumOfJSSideSize {
        static String outFile = "/your/dir/to/jsSide/popularRNAppsJS.csv";
    
        public static void main(String[] args) {
    
            String jsonFile = "/your/dir/to/AndroZoo/mostPopularRN/uniquePopularRN_hermes_ID.json";        
            String appFolder = "/your/dir/to/AndroZoo/mostPopularRN/apps";
            String outputFolder = "/your/dir/to/reactNativeOutput/popularRNAppsFromAndroZoo/crossLanguage/jsSide/results";
    
            
            String apkPath;

            String androidJars = "/your/dir/to/android-platforms";
            String sourcesAndSinks = "/your/dir/to/reactNativeInput/flowdroid/SourcesAndSinks.txt";
            JSONArray sha256Array; 
    
            try (FileReader reader = new FileReader(jsonFile)) {
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(reader);
                sha256Array = (JSONArray) obj;              
            } catch (IOException | ParseException e) {
                sha256Array = new JSONArray();
                e.printStackTrace();
            }
    
    
            int count = 0;
    
            for (Object o : sha256Array) {
                
                count++;
                String sha256 = o.toString(); 
                
                if(readFromCSV(outFile).contains(sha256)){
                    continue;
                }
                ReactUtil.clearReactUtil();

                System.out.println("+++++++++++++++++++++++++++++" +"Currently, working on: " + count + "+++++++++++++++"+ sha256 + "+++++++++++++++++++++++++++++");
    
                String[] data = new String[6];
                data[0] = sha256;
    
                apkPath = appFolder + "/" + sha256 + ".apk";


                SetupApplication setupApplication = new SetupApplication(androidJars, apkPath);
                IInfoflowConfig sootConfig = new SootConfigForAndroidRN("/your/dir/to/HermesTool");
                setupApplication.setSootConfig(sootConfig);

                InfoflowAndroidConfiguration config = setupApplication.getConfig();

                try {    
                    config.setMergeDexFiles(true);
                    config.setDataFlowTimeout(400);
                    config.getCallbackConfig().setCallbackAnalysisTimeout(400);
                    config.getPathConfiguration().setPathReconstructionTimeout(400);
                    setupApplication.addPreprocessor(new ReactJSModulesCount(setupApplication, outputFolder, sha256));


                    InfoflowResults infoflowResults = setupApplication.runInfoflow(sourcesAndSinks);

                   
    
                } catch ( RuntimeException  | OutOfMemoryError | IOException | XmlPullParserException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    data[1] = "unknown";
                    data[2] = "unknown";
                    data[3] = "unknown";
                    data[4] = "unknown";
                    data[5] = "unknown";

                    if(readFromCSV(outFile).contains(sha256)){
                        continue;
                    }else{
                        addCSVRow(data, outFile);
                    }                        
                }
                            
            }    
        }
    
        public static List<String> readFromCSV(String fileDir){
            String line = "";
            String csvDelimiter = ",";
            List<String> sha256List = new ArrayList<String>();
            try (BufferedReader br = new BufferedReader(new FileReader(fileDir))) {
                            
                // Read the remaining lines (data)
                while ((line = br.readLine()) != null) {
                    // Split the line using the specified delimiter
                    String[] data = line.split(csvDelimiter);
                    
                    sha256List.add(data[0]);
                }
                
            } catch (IOException e) {
                writeAndConstructCSV(fileDir);
                e.printStackTrace();
            }
    
            return sha256List;
        }
    
        
    
        public static void writeAndConstructCSV(String fileDir){
            try {
                // create the CSVWriter object
                FileWriter writer = new FileWriter(fileDir);
                
                // write the header row
                writer.write("sha256,JavaAPITypeNum,JavaInvocationNum,numHbcMethods,numJimpleGenerated,totalInvocationInstructionNum");
                // close the writer
                writer.close();
                System.out.println("Data written to CSV file successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
    
    
        public static void addCSVRow(String[] rowData, String fileDir){
            String csvFile = fileDir;
            FileWriter writer = null;
            try {
                writer = new FileWriter(csvFile, true); // true to append data to file
                writer.write("\n"); // start a new row
                writer.write(rowData[0]+","+rowData[1]+","+rowData[2]+","+rowData[3]+","+rowData[4]+","+rowData[5]); // write the new row
    
                System.out.println("New row added to CSV file");
            } catch (IOException e) {
                System.out.println("Error adding new row to CSV file: " + e.getMessage());
            } finally {
                try {
                    writer.close(); // close the CSV file
                } catch (IOException e) {
                    System.out.println("Error closing CSV file: " + e.getMessage());
                }
            }
        }
}
    