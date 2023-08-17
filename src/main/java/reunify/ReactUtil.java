package reunify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.hermeser.text.HbcTypeFactory;
import soot.hermeser.text.InvocationItem;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;

public class ReactUtil {
    protected static Map<SootClass,Collection<SootMethod>> reactModules;
    protected static Map<SootClass,Collection<SootMethod>> reactViews;

    protected static Map<SootMethod, List<InvocationItem>> javaInvocationInHBC = new HashMap<>();
    protected static Map<String, SootMethod> modulesAPIMap = new HashMap<>();


    protected static boolean isHermesMethodSourced = false;

    protected static Collection<SootMethod> reactMethods;
    public static Boolean isReactModulesDetected = false;
    protected static Boolean isReactEntryPointDetected = false;
    protected static Boolean isReactViewsDetected = false;


    protected static int numberInvoked = 0;
    public static SootMethod reactEntryMethod;
    public static SootMethod reactViewsEntryPoint;
    public static SootMethod reactModulesEntryMethod;

    public static SootClass hbcClass;


    protected static List<InvocationItem> jsAPIList = new ArrayList<>();

    public static void clearReactUtil(){
        reactModules =null;
        reactViews = null;
        javaInvocationInHBC = new HashMap<>();
        modulesAPIMap = new HashMap<>();


        isHermesMethodSourced = false;

        reactMethods = null;
        isReactModulesDetected = false;
        isReactEntryPointDetected = false;
        isReactViewsDetected = false;


        numberInvoked = 0;
        reactEntryMethod = null;
        reactViewsEntryPoint = null;
        reactModulesEntryMethod = null;

        hbcClass = null;


        jsAPIList = new ArrayList<>();

    }



    public static Map<SootMethod, List<InvocationItem>> getJavaInvocationInHBC() {
        return javaInvocationInHBC;
    }


    public static void constructModulesAPIMap(){
        for(SootClass reactModule: reactModules.keySet()){
            if(reactModule.isAbstract()){
                continue;
            }
            
            if(!reactModule.declaresMethod("java.lang.String getName()"))
                continue;

            SootMethod getNameMethod = reactModule.getMethod("java.lang.String getName()");
            Unit returnUnit = getNameMethod.retrieveActiveBody().getUnits().getLast();
            Collection<SootMethod> reactMethodsInModule = reactModules.get(reactModule);
            String APIFullName =returnUnit.getUseBoxes().get(0).getValue().toString();
            String APIname =  APIFullName.substring(1, APIFullName.length()-1);

            for(SootMethod reactMethod: reactMethodsInModule){
                String methodName = reactMethod.getName();
                modulesAPIMap.put(APIname+"."+methodName, reactMethod);
            }
        }
    }



    // construct the String2InvocationItem map
    public static void constructString2InvocationItemMap(){
        List<InvocationItem> invocationItems = Scene.v().getHermesScene().getHermesAssemblyFile().getHasmFileDefinition().getCalleeTypeList();

        for(InvocationItem invocationItem: invocationItems){
            if(invocationItem == null){
                continue;
            }
            String calleeType = invocationItem.getCalleeType();
            String[] calleeTypeSplit = calleeType.split("\\.");

            String currentInvocationType = calleeTypeSplit[calleeTypeSplit.length-2]+"."+calleeTypeSplit[calleeTypeSplit.length-1];
            SootMethod detectedMSootMethod = modulesAPIMap.get(currentInvocationType);

            if(detectedMSootMethod==null){
                continue;
            }

            if(javaInvocationInHBC.containsKey(detectedMSootMethod)){
                javaInvocationInHBC.get(detectedMSootMethod).add(invocationItem);
            }
            
            else{
                List<InvocationItem> invocationItemList = new ArrayList<>();
                invocationItemList.add(invocationItem);
                javaInvocationInHBC.put(detectedMSootMethod, invocationItemList);
            }
        }
    }



    // if the react method detection process has calculated before, then return the react methods
    public static Collection<SootMethod> getReactMethods() {
        if(isReactEntryPointDetected){
            return reactMethods;
        }
        else{
            reactMethods = extractReactMethods();
            isReactEntryPointDetected = true;
            return reactMethods;
        }
    }

    
    public static SootClass getHbcClass(){
        if(isHermesMethodSourced){
            return hbcClass;
        }
        else{
            hbcClass = extractHbcClass();
            isHermesMethodSourced = true;
            return hbcClass;
        }
    }
    
    // if the react modules detection process has run before, then return the react methods
    public static Map<SootClass,Collection<SootMethod>> getReactModules() {
        if(isReactModulesDetected){
            return reactModules;
        }
        else{
            reactModules = extractReactModules();
            isReactModulesDetected = true;
            return reactModules;
        }
    }

    // if the react views detection process has run before, then return the react methods
    public static Map<SootClass,Collection<SootMethod>> getReactViews() {
        if(isReactViewsDetected){
            return reactViews;
        }
        else{
            reactViews = extractReactViews();
            isReactViewsDetected = true;
            return reactViews;
        }
    }

    // extract @ react methods from the scene
    public static Collection<SootMethod> extractReactMethods(){

        Chain<SootClass> sootClasses = Scene.v().getClasses();
        Collection<SootMethod> reactMethodTem =new ArrayList<>();


        for (Iterator<SootClass> iter = sootClasses.snapshotIterator(); iter.hasNext();)
        {
            SootClass sc = iter.next();

            List<SootMethod> sootMethodsTem = sc.getMethods();

            for (int i = 0; i < sootMethodsTem.size(); i++)
            {
                SootMethod sm = sootMethodsTem.get(i);

                VisibilityAnnotationTag tag = (VisibilityAnnotationTag) sm.getTag("VisibilityAnnotationTag");

                if (tag !=null) {
                    boolean isReactMethod = false;
                    for (AnnotationTag annotation : tag.getAnnotations()) {
                        if (annotation.getType().equals("Lcom/facebook/react/bridge/ReactMethod;")) {
                            isReactMethod= true;
                        }
                    }
                    if (isReactMethod){
                        reactMethodTem.add(sm);
                    }
                }
            }
        }
        return  reactMethodTem;
    }

    // extract @ react methods from the scene
    public static Map<SootClass, Collection<SootMethod>> extractReactModules(){
        Map<SootClass, Collection<SootMethod>> outputs = new HashMap<SootClass, Collection<SootMethod>>();
        List<SootClass> reactModulePossilbleClassList = new ArrayList<SootClass>();

        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        
        if(Scene.v().containsType("com.facebook.react.bridge.ReactContextBaseJavaModule")){
            SootClass reactContextBaseJavaModuleClass = Scene.v().getSootClass("com.facebook.react.bridge.ReactContextBaseJavaModule");
            List<SootClass> reactContextBaseJavaModuleSubClassList = hierarchy.getSubclassesOf(reactContextBaseJavaModuleClass);
            reactModulePossilbleClassList.addAll(reactContextBaseJavaModuleSubClassList);
        }

        

        if(Scene.v().containsType("com.facebook.react.bridge.ContextBaseJavaModule")){
            SootClass contextBaseJavaModuleClass = Scene.v().getSootClass("com.facebook.react.bridge.ContextBaseJavaModule");
            List<SootClass> contextBaseJavaModuleSubClassList = hierarchy.getSubclassesOf(contextBaseJavaModuleClass);
            reactModulePossilbleClassList.addAll(contextBaseJavaModuleSubClassList);
        }



        SootClass reactModuleWithSpecClass = Scene.v().getSootClass("com.facebook.react.bridge.ReactModuleWithSpec");
        SootClass turboModuleClass = Scene.v().getSootClass("com.facebook.react.turbomodule.core.interfaces.TurboModule");

        ArrayList<SootClass> reactNewArchModuleClasses = new ArrayList<>();
        ArrayList<SootClass> reactOldArchModuleClasses = new ArrayList<>();

        
        ArrayList<SootClass> reactExtendedModules = new ArrayList<>();


        for (SootClass sootClass : reactModulePossilbleClassList)
        {   
            if(hierarchy.getSubclassesOf(sootClass).size()>0){
                // new Architecture Spec detect
                if(implementsInterfaces(sootClass, Arrays.asList(reactModuleWithSpecClass, turboModuleClass))){
                    reactNewArchModuleClasses.add(sootClass);
                }
            }
            // final Module Architecture detect
            else{
                if(sootClass.getSuperclassUnsafe().getName().equals("com.facebook.react.bridge.ReactContextBaseJavaModule") || sootClass.getSuperclassUnsafe().getName().equals("com.facebook.react.bridge.ContextBaseJavaModule")){
                    reactOldArchModuleClasses.add(sootClass);
                }else{
                    reactExtendedModules.add(sootClass);
                }
            }
        }
        
        // deal with the new architecture
        for (SootClass sootNewArchClass : reactNewArchModuleClasses){
            for (int i = 0; i < reactExtendedModules.size(); i++){
                SootClass extentedSootClass = reactExtendedModules.get(i);
                if(hierarchy.isClassSubclassOf(extentedSootClass, sootNewArchClass)){                   
                    Collection<SootMethod> moduleMethods = extractReactMethodsFromNewArch(sootNewArchClass , extentedSootClass);
                    outputs.put(extentedSootClass, moduleMethods);
                    reactExtendedModules.remove(i);
                    break;
                }
            }
        }

        reactOldArchModuleClasses.addAll(reactExtendedModules);

        // deal with the old architecture
        for (SootClass sootOldArchClass : reactOldArchModuleClasses){
            Collection<SootMethod> moduleMethods = extractReactMethodsFromClass(sootOldArchClass);
            outputs.put(sootOldArchClass, moduleMethods);
        }
        
        return  outputs;
    }


    public static Collection<SootMethod> extractReactMethodsFromNewArch(SootClass sootNewArchClass, SootClass extentedSootClass) {            
        Collection<SootMethod> reactMethodSigs =new ArrayList<>();
        Collection<SootMethod> reactAbstractMethodSigInNewArchClass = extractReactMethodsFromClass(sootNewArchClass);

        
        for (SootMethod sootNewArchMethod : reactAbstractMethodSigInNewArchClass)
        {
            if(extentedSootClass.declaresMethod(sootNewArchMethod.getSubSignature())){
                reactMethodSigs.add(extentedSootClass.getMethod(sootNewArchMethod.getSubSignature()));                        
            }
        }
        return reactMethodSigs;
    }
    
    // convert the full signature to the sub signature
    public static String fullSig2SubSig(String sootNewArchMethodFullSig) {
        return sootNewArchMethodFullSig.substring(0, sootNewArchMethodFullSig.length()-1).split(":")[1].trim();
    }

    // extract @ react methods from the target SootClass
    public static Collection<SootMethod> extractReactMethodsFromClass(SootClass sootClass) {            
        Collection<SootMethod> reactMethods=new ArrayList<>();

        for (SootMethod sootMethod : sootClass.getMethods())
        {

            VisibilityAnnotationTag tag = (VisibilityAnnotationTag) sootMethod.getTag("VisibilityAnnotationTag");

            if (tag !=null) {
                boolean isReactMethod = false;
                for (AnnotationTag annotation : tag.getAnnotations()) {
                    if (annotation.getType().equals("Lcom/facebook/react/bridge/ReactMethod;")) {
                        isReactMethod= true;
                    }
                }
                if (isReactMethod){                        
                    reactMethods.add(sootMethod);                        
                }
            }
        }
        return reactMethods;
    }

    // check if the target SootClass implements the target interfaces
    public static Boolean implementsInterfaces(SootClass sootClass, List<SootClass> interfaceClasses){
        for(SootClass interfaceClass : interfaceClasses){
            if(!sootClass.implementsInterface(interfaceClass.toString())){
                return false;
            }
        }
        return true;
    }


    // extract react view from the scene
    public static Map<SootClass, Collection<SootMethod>> extractReactViews(){
        Map<SootClass, Collection<SootMethod>> reactViews = new HashMap<SootClass, Collection<SootMethod>>();

        Chain<SootClass> sootClasses = Scene.v().getClasses();


        for (Iterator<SootClass> iter = sootClasses.snapshotIterator(); iter.hasNext();)
        {
            SootClass sc = iter.next();

            List<SootMethod> sootMethodsTem = sc.getMethods();
            Collection<SootMethod> reactPropsNameTem =new ArrayList<>();

            for (int i = 0; i < sootMethodsTem.size(); i++)
            {
                SootMethod sm = sootMethodsTem.get(i);

                VisibilityAnnotationTag tag = (VisibilityAnnotationTag) sm.getTag("VisibilityAnnotationTag");

                if (tag !=null) {
                    boolean isReactProp = false;
                    for (AnnotationTag annotation : tag.getAnnotations()) {
                        if (annotation.getType().equals("Lcom/facebook/react/uimanager/annotations/ReactProp;") || annotation.getType().equals("Lcom/facebook/react/uimanager/annotations/ReactPropGroup;") ) {
                            isReactProp= true;
                        }
                    }
                    if (isReactProp){
                        if (!reactViews.containsKey(sc)){
                            reactViews.put(sc, reactPropsNameTem);
                        };
                        reactPropsNameTem.add(sm);
                    }
                }
            }
        }
        return  reactViews;
    }





    public static SootClass extractHbcClass(){
        SootClass cl = Scene.v().getSootClass(HbcTypeFactory.JAVASCRIPT_HERMES);
        

        int threadNum = Options.v().coffi() ? 1 : Runtime.getRuntime().availableProcessors();

        System.out.println("threadNum: " + threadNum);
        ExecutorService executor = new ThreadPoolExecutor(threadNum, threadNum, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        List<CompletableFuture<Void>> futures = new ArrayList<>();


        for (SootMethod m : new ArrayList<SootMethod>(cl.getMethods())) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println(m.getName());
                m.retrieveActiveBody(); 
                // Do some work asynchronously
            }, executor).thenRunAsync(() -> {
                m.getActiveBody().validate(); 
                // Do some more work asynchronously
            }, executor);

            futures.add(future);
        }

        
        
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            executor.shutdown();
        }



        return cl;
    }
}
