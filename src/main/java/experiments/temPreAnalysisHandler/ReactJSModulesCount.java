package experiments.temPreAnalysisHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.simple.JSONObject;

import reunify.ReactUtil;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.hermeser.text.InvocationItem;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;

public class ReactJSModulesCount implements PreAnalysisHandler {
    SetupApplication app;
    String resultsDir;
    String sha256;

    static String outFile = "/your/dir/to/reactNativeOutput/popularRNAppsFromAndroZoo/crossLanguage/jsSide/popularRNAppsJS.csv";    

    public ReactJSModulesCount(SetupApplication app, String resultsDir, String sha256) {
        this.app = app;
        this.resultsDir = resultsDir;
        this.sha256 = sha256;
    }

    @Override
    public void onBeforeCallgraphConstruction() {
        String[] result = new String[6];
        String jsonFilePath = resultsDir + "/" + sha256 + ".json";

        result[0] = sha256;

        try {
            SootClass hbcClass = reunify.ReactUtil.getHbcClass();
            System.out.println(hbcClass.getMethods().size());

            if (!ReactUtil.isReactModulesDetected) {
                System.out.println("React entry point is created");
                // get all the react modules
                Map<SootClass, Collection<SootMethod>> reactMethods = ReactUtil.getReactModules();

                ReactUtil.constructModulesAPIMap();
                ReactUtil.constructString2InvocationItemMap();

                int javaAPIInvocationNum = 0;
                int javaInvocationTypeNum = ReactUtil.getJavaInvocationInHBC().size();

                JSONObject outputObject = new JSONObject();
                // connect the cross-language calls
                for (Entry<SootMethod, List<InvocationItem>> javaInvocationInHbcItem : ReactUtil
                        .getJavaInvocationInHBC().entrySet()) {
                    javaAPIInvocationNum += javaInvocationInHbcItem.getValue().size();
                    List<String> reactNativeJSMethod = new ArrayList<String>();

                    for (InvocationItem invocationItem : javaInvocationInHbcItem.getValue()) {
                        reactNativeJSMethod.add(invocationItem.getCalleeType());
                    }
                    outputObject.put(javaInvocationInHbcItem.getKey().getName(), reactNativeJSMethod);
                }

                try (PrintWriter out = new PrintWriter(new FileWriter(jsonFilePath))) {
                    out.write(outputObject.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int sizeOfReactNativeJSUnits = 0;
                for (SootMethod sootMethod : hbcClass.getMethods()) {
                    sizeOfReactNativeJSUnits += sootMethod.getActiveBody().getUnits().size();
                }
                result[1] = Integer.toString(javaInvocationTypeNum);
                result[2] = Integer.toString(javaAPIInvocationNum);
                result[3] = Integer.toString(hbcClass.getMethods().size());
                result[4] = Integer.toString(sizeOfReactNativeJSUnits);
                result[5] = Integer.toString(Scene.v().getHermesScene().getHermesAssemblyFile().getHasmFileDefinition().getCalleeTypeList().size());

            }
        } catch (Exception e) {
            e.printStackTrace();
            result[1] = "unknown";
            result[2] = "unknown";
            result[3] = "unknown";
            result[4] = "unknown";
            result[5] = "unknown";
        }

        updateCSVRow(result, outFile);
    

        app.abortAnalysis();
        throw new RuntimeException("Analysis aborted");

    }

    @Override
    public void onAfterCallgraphConstruction() {
        // TODO Auto-generated method stub

    }

    public static void updateCSVRow(String[] rowData, String fileUpdate){
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
                    if (line[0].equals(rowData[0])) { // found target row
                        writer.write(String.join(",", rowData) + "\n"); // write updated row to file
                    } else {
                        writer.write(String.join(",", line) + "\n"); // write non-target rows to file
                    }
                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }


    public void addCSVRow(String[] rowData, String fileDir){
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
