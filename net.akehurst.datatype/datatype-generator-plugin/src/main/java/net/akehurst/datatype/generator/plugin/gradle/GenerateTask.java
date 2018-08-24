/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.datatype.generator.plugin.gradle;

import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.io.FileTemplateLoader;

import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeSignature;
import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;

public class GenerateTask extends DefaultTask {

    public static final String NAME = "generate";
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateTask.class);

    private File templateFile;
    private File outputFile;
    public List<String> classPatterns;
    public Map<String, String> typeMapping;

    public GenerateTask() {
        this.setGroup("Datatype");
        this.setDescription("Generate from defined java (@Datatype) classes");
    }

    @InputFile
    public File getTemplateFile() {
        return this.templateFile;
    }

    public void setTemplateFile(final File value) {
        this.templateFile = value;
    }

    @Input
    public List<String> getClassPatterns() {
        return this.classPatterns;
    }

    public void setClassPatterns(final List<String> value) {
        this.classPatterns = value;
    }

    @Input
    public Map<String, String> getTypeMapping() {
        return this.typeMapping;
    }

    public void setTypeMapping(final Map<String, String> value) {
        this.typeMapping = value;
    }

    @OutputFile
    public File getOutputFile() {
        return this.outputFile;
    }

    public void setOutputFile(final File value) {
        this.outputFile = value;
    }

    @TaskAction
    void exec() {
        GenerateTask.LOGGER.info("Executing " + GenerateTask.NAME);

        final Map<String, Object> model = this.createModel(this.getClassPatterns());

        this.generate(model, this.getTemplateFile(), this.getOutputFile());

    }

    private void generate(final Map<String, Object> model, final File templateFile, final File outputFile) {
        super.getLogger().info("Generating ", templateFile, outputFile);
        // final DefaultMustacheFactory mf = new DefaultMustacheFactory();

        GenerateTask.LOGGER.info("Using template directory " + templateFile.getParentFile());
        final FileTemplateLoader ftl = new FileTemplateLoader(templateFile.getParentFile(), "");
        final Handlebars hb = new Handlebars(ftl);

        try {
            GenerateTask.LOGGER.info("Compiling template " + templateFile.getName());
            final Template t = hb.compile(templateFile.getName());

            GenerateTask.LOGGER.info("Generating output");
            final FileWriter writer = new FileWriter(outputFile);

            final Context ctx = Context.newBuilder(model).resolver(MapValueResolver.INSTANCE).build();

            t.apply(ctx, writer);
            writer.close();
        } catch (final Exception e) {
            GenerateTask.LOGGER.error("Unable to generate", e);
        }
    }

    private Map<String, Object> createModel(final List<String> classPatterns) {
        final Map<String, Object> model = new HashMap<>();
        final List<Map<String, Object>> datatypes = new ArrayList<>();
        model.put("datatype", datatypes);

        GenerateTask.LOGGER.info("Scaning classpath");
        final URLClassLoader cl = this.createClassLoader();
        final ScanResult scan = new ClassGraph()//
                .addClassLoader(cl)//
                .enableAllInfo()//
                // .verbose()//
                .enableExternalClasses() //
                .enableAnnotationInfo() //
                .enableSystemPackages()//
                .whitelistPackages(classPatterns.toArray(new String[classPatterns.size()]))//
                .scan();

        GenerateTask.LOGGER.debug("Found " + scan.getAllClasses().size());
        GenerateTask.LOGGER.info("Building Datatype model");
        final ClassInfoList dtClassInfo = scan.getClassesWithAnnotation(Datatype.class.getName());

        Collections.sort(dtClassInfo, (final ClassInfo c1, final ClassInfo c2) -> {
            if (c1.getSuperclasses().contains(c2)) {
                return 1;
            } else if (c2.getSuperclasses().contains(c1)) {
                return -1;
            } else {
                final int res = 0;

                return res;
            }
        });

        for (final ClassInfo cls : dtClassInfo) {
            GenerateTask.LOGGER.debug("Adding Datatype " + cls.getName());
            final Map<String, Object> dtModel = new HashMap<>();
            datatypes.add(dtModel);
            dtModel.put("name", cls.getName().substring(cls.getName().lastIndexOf(".") + 1));
            dtModel.put("fullName", cls.getName());
            dtModel.put("isInterface", cls.isInterface());
            dtModel.put("isAbstract", cls.isAbstract());

            final List<Map<String, Object>> extends_ = new ArrayList<>();
            dtModel.put("extends", extends_);
            if (null != cls.getSuperclass() && cls.getSuperclass().hasAnnotation(Datatype.class.getName())) {
                extends_.add(this.createPropertyType(cls.getSuperclass(), new ArrayList<>(), false));
            }
            final List<Map<String, Object>> implements_ = new ArrayList<>();
            dtModel.put("implements", implements_);
            for (final ClassInfo i : cls.getInterfaces()) {
                if (i.hasAnnotation(Datatype.class.getName())) {
                    implements_.add(this.createPropertyType(i, new ArrayList<>(), false));
                }
            }

            final List<Map<String, Object>> property = new ArrayList<>();
            dtModel.put("property", property);
            for (final MethodInfo mi : cls.getMethodInfo()) {
                final boolean isRef = mi.getAnnotationInfo().containsName(Reference.class.getName());
                if (mi.getAnnotationInfo().containsName(Query.class.getName())) {
                    // ignore for now!
                } else {
                    if (mi.getParameterInfo().length == 0 && mi.getName().startsWith("get")) {
                        final Map<String, Object> meth = new HashMap<>();
                        meth.put("name", this.createMemberName(mi.getName()));
                        final TypeSignature propertyTypeSig = mi.getTypeSignatureOrTypeDescriptor().getResultType();
                        meth.put("type", this.createPropertyType(propertyTypeSig, isRef));
                        property.add(meth);
                    }
                }
            }
        }

        return model;
    }

    private URLClassLoader createClassLoader() {
        final List<URL> urls = new ArrayList<>();

        final Configuration c = this.getProject().getConfigurations().findByName("default");
        for (final File file : c.resolve()) {
            try {
                GenerateTask.LOGGER.debug("Adding url for " + file);
                urls.add(file.toURI().toURL());
            } catch (final MalformedURLException e) {
                GenerateTask.LOGGER.error("Unable to create url for " + file, e);
            }
        }

        return new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
    }

    private String createMemberName(final String methodName) {
        return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
    }

    private Map<String, Object> createPropertyType(final ClassInfo ci, final List<TypeArgument> tArgs, final boolean isRef) {
        final Map<String, Object> mType = new HashMap<>();
        final String fullName = ci.getName();
        mType.put("fullName", fullName);
        mType.put("name", fullName.substring(fullName.lastIndexOf('.') + 1));
        if (ci.implementsInterface("java.util.Collection")) {
            mType.put("isCollection", true);
            mType.put("isOrdered", Objects.equals("java.util.List", ci.getName()));

            final Map<String, Object> et = this.createPropertyType(tArgs.get(0).getTypeSignature(), false); // what is isRef here!
            GenerateTask.LOGGER.info("Collection with elementType " + tArgs.get(0).getTypeSignature());
            mType.put("elementType", et);

        } else if (ci.isEnum()) {
            mType.put("isEnum", true);
        }
        mType.put("isReference", isRef);
        return mType;
    }

    private Map<String, Object> createPropertyType(final TypeSignature typeSig, final boolean isRef) {
        Map<String, Object> mType = new HashMap<>();
        if (typeSig instanceof BaseTypeSignature) {
            final BaseTypeSignature bts = (BaseTypeSignature) typeSig;
            mType.put("name", bts.getTypeStr());
            mType.put("fullName", bts.getTypeStr());
        } else if (typeSig instanceof ArrayTypeSignature) {
            final ArrayTypeSignature rts = (ArrayTypeSignature) typeSig;
            mType.put("name", "array");
            mType.put("fullName", "array");
            mType.put("isCollection", true);
            mType.put("elementType", this.createPropertyType(rts.getElementTypeSignature(), isRef));
        } else if (typeSig instanceof ClassRefTypeSignature) {
            final ClassRefTypeSignature cts = (ClassRefTypeSignature) typeSig;
            if (null == cts.getClassInfo()) {
                final String fullName = cts.getFullyQualifiedClassName();
                mType.put("fullName", fullName);
                mType.put("name", fullName.substring(fullName.lastIndexOf('.') + 1));
                GenerateTask.LOGGER.error("Unable to find class info for " + cts.getFullyQualifiedClassName());
            } else {
                mType = this.createPropertyType(cts.getClassInfo(), cts.getTypeArguments(), isRef);
            }
        } else {
        }

        final String mappedName = this.getTypeMapping().get(mType.get("fullName"));
        if (null == mappedName) {
            return mType;
        } else {
            GenerateTask.LOGGER.debug("Mapping " + mType.get("name") + " to " + mappedName);
            mType.put("name", mappedName);
            return mType;
        }

    }

}
