package com.chaitin.jar.analyzer.adapter;

import com.chaitin.jar.analyzer.core.ClassReference;
import com.chaitin.jar.analyzer.core.MethodReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import com.chaitin.jar.analyzer.model.ClassObj;
import com.chaitin.jar.analyzer.model.MappingObj;
import com.chaitin.jar.analyzer.model.MethodObj;
import com.chaitin.jar.analyzer.model.ResObj;
import com.chaitin.jar.analyzer.util.OSUtil;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import org.benf.cfr.reader.Main;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.objectweb.asm.Type;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MappingMouseAdapter extends MouseAdapter {

    private final JarAnalyzerForm form;

    public MappingMouseAdapter(JarAnalyzerForm form) {
        this.form = form;
    }

    public void mousePressed(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (SwingUtilities.isRightMouseButton(evt) || evt.isControlDown()) {
            int index = list.locationToIndex(evt.getPoint());
            MappingObj res = (MappingObj) list.getModel().getElementAt(index);
            JarAnalyzerForm.chainDataList.addElement(res.getResObj());
            form.chanList.setModel(JarAnalyzerForm.chainDataList);
        }
    }

    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 1) {
            JList<?> l = (JList<?>) evt.getSource();
            ListModel<?> m = l.getModel();
            int index = l.locationToIndex(evt.getPoint());
            if (index > -1) {
                MappingObj res = (MappingObj) m.getElementAt(index);
                l.setToolTipText(res.getResObj().getMethod().getDescStd(res));

                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(l, 0, 0, 0,
                                evt.getX(), evt.getY(), 0, false));
            }
        } else if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            MappingObj res = (MappingObj) list.getModel().getElementAt(index);

            String className = res.getResObj().getClassName();
            String tempPath = className.replace("/", File.separator);
            String classPath;
            classPath = String.format("temp%s%s.class", File.separator, tempPath);
            if (!Files.exists(Paths.get(classPath))) {
                classPath = String.format("temp%sBOOT-INF%sclasses%s%s.class",
                        File.separator, File.separator, File.separator, tempPath);
                if (!Files.exists(Paths.get(classPath))) {
                    classPath = String.format("temp%sWEB-INF%sclasses%s%s.class",
                            File.separator, File.separator, File.separator, tempPath);
                    if (!Files.exists(Paths.get(classPath))) {
                        JOptionPane.showMessageDialog(form.jarAnalyzerPanel, "Missing dependencies");
                        return;
                    }
                }
            }

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            OutputStreamWriter ows = new OutputStreamWriter(bao);
            BufferedWriter writer = new BufferedWriter(ows);

            String finalClassPath = classPath;

            String[] temp;
            if (OSUtil.isWindows()) {
                temp = finalClassPath.split(File.separator + File.separator);
            } else {
                temp = finalClassPath.split(File.separator);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < temp.length - 1; i++) {
                sb.append(temp[i]);
                sb.append(File.separator);
            }
            String javaDir = sb.toString();
            String tempName = temp[temp.length - 1].split("\\.class")[0];
            String javaPath = sb.append(tempName).append(".java").toString();
            Path javaPathPath = Paths.get(javaPath);

            new Thread(() -> {
                String total;
                JarAnalyzerForm.historyDataList.addElement(res.getResObj());
                if (form.procyonRadioButton.isSelected()) {
                    Decompiler.decompile(
                            finalClassPath,
                            new PlainTextOutput(writer));
                    try {
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    total = bao.toString();
                    if (total == null || total.trim().equals("")) {
                        total = JarAnalyzerForm.tips;
                    } else {
                        total = "// Procyon \n" + total;
                    }
                } else if (form.quiltFlowerRadioButton.isSelected()) {
                    String[] args = new String[]{
                            finalClassPath,
                            javaDir
                    };
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                    ConsoleDecompiler.main(args);
                    try {
                        total = new String(Files.readAllBytes(javaPathPath));
                        if (total.trim().equals("")) {
                            total = JarAnalyzerForm.tips;
                        } else {
                            total = "// QuiltFlower \n" + total;
                        }
                    } catch (Exception ignored) {
                        total = "";
                    }
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                } else if (form.cfrRadioButton.isSelected()) {
                    String[] args = new String[]{
                            finalClassPath,
                            "--outputpath",
                            "temp"
                    };
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                    Main.main(args);
                    try {
                        if (finalClassPath.contains("BOOT-INF")) {
                            total = new String(Files.readAllBytes(
                                    Paths.get(String.format("temp%s%s", File.separator,
                                            javaPathPath.toString().substring(22)))
                            ));
                        } else if (finalClassPath.contains("WEB-INF")) {
                            total = new String(Files.readAllBytes(
                                    Paths.get(String.format("temp%s%s", File.separator,
                                            javaPathPath.toString().substring(21)))
                            ));
                        } else {
                            total = new String(Files.readAllBytes(javaPathPath));
                        }
                    } catch (Exception ignored) {
                        total = "";
                    }
                    if (total.trim().equals("")) {
                        total = JarAnalyzerForm.tips;
                    }
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Can not decompile");
                    return;
                }
                total = total.replace("\r\n", "\n");
                form.editorPane.setText(total);

                // 目标方法名是构造
                String methodName = res.getResObj().getMethod().getName();
                if (methodName.equals("<init>")) {
                    String[] c = res.getResObj().getClassName().split("/");
                    // 方法名应该是类名
                    methodName = c[c.length - 1];
                }

                int paramNum = Type.getMethodType(
                        res.getResObj().getMethod().getDesc()).getArgumentTypes().length;
                int pos = form.find(total, methodName, paramNum);
                form.editorPane.setCaretPosition(pos + 1);
            }).start();

            DefaultListModel<ResObj> sourceDataList = new DefaultListModel<>();
            DefaultListModel<ResObj> callDataList = new DefaultListModel<>();
            DefaultListModel<ClassObj> subDataList = new DefaultListModel<>();
            DefaultListModel<ClassObj> superDataList = new DefaultListModel<>();
            DefaultListModel<MethodObj> allMethodsList = new DefaultListModel<>();

            MethodReference.Handle handle = res.getResObj().getMethod();
            HashSet<MethodReference.Handle> callMh = JarAnalyzerForm.methodCalls.get(handle);
            for (MethodReference.Handle m : callMh) {
                callDataList.addElement(new ResObj(m, m.getClassReference().getName()));
            }
            for (Map.Entry<MethodReference.Handle,
                    HashSet<MethodReference.Handle>> entry : JarAnalyzerForm.methodCalls.entrySet()) {
                MethodReference.Handle mh = entry.getKey();
                HashSet<MethodReference.Handle> mSet = entry.getValue();
                for (MethodReference.Handle m : mSet) {
                    if (m.getClassReference().getName().equals(className)) {
                        if (m.getName().equals(res.getResObj().getMethod().getName())) {
                            sourceDataList.addElement(new ResObj(
                                    mh, mh.getClassReference().getName()));
                        }
                    }
                }
            }

            Set<ClassReference.Handle> subClasses = JarAnalyzerForm.inheritanceMap.getSubClasses(handle.getClassReference());
            if (subClasses != null && subClasses.size() != 0) {
                for (ClassReference.Handle c : subClasses) {
                    ClassObj obj = new ClassObj(c.getName(), c);
                    subDataList.addElement(obj);
                }
            }

            Set<ClassReference.Handle> superClasses = JarAnalyzerForm.inheritanceMap.getSuperClasses(handle.getClassReference());
            if (superClasses != null && superClasses.size() != 0) {
                for (ClassReference.Handle c : superClasses) {
                    ClassObj obj = new ClassObj(c.getName(), c);
                    superDataList.addElement(obj);
                }
            }

            List<MethodReference> mList = JarAnalyzerForm.methodsInClassMap.get(handle.getClassReference());

            Map<String, MethodObj> tempResults = new LinkedHashMap<>();

            for (MethodReference m : mList) {
                MethodObj resObj = new MethodObj(m.getHandle(), m.getClassReference().getName());
                tempResults.put(resObj.toString(), resObj);
            }

            List<String> keySet = new ArrayList<>(tempResults.keySet());
            Collections.sort(keySet);

            for (String s : keySet) {
                MethodObj obj = tempResults.get(s);
                if (obj.getMethod().getName().startsWith("lambda$")) {
                    continue;
                }
                if (obj.getMethod().getName().startsWith("access$")) {
                    continue;
                }
                if (obj.getMethod().getName().equals("<clinit>")) {
                    continue;
                }
                allMethodsList.addElement(tempResults.get(s));
            }

            JarAnalyzerForm.curRes = res.getResObj();
            form.currentLabel.setText(res.toString());
            form.currentLabel.setToolTipText(res.getResObj().getMethod().getDescStd(JarAnalyzerForm.curRes));

            form.sourceList.setModel(sourceDataList);
            form.callList.setModel(callDataList);
            form.subList.setModel(subDataList);
            form.superList.setModel(superDataList);
            form.allMethodList.setModel(allMethodsList);
            form.historyList.setModel(JarAnalyzerForm.historyDataList);
        }
    }
}
