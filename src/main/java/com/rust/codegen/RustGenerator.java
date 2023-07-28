package com.rust.codegen;

import com.google.gson.Gson;
import com.ibm.icu.text.Transliterator;
import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.File;

import static io.swagger.codegen.v3.CodegenConstants.*;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class RustGenerator extends DefaultCodegenConfig {

  static Logger LOGGER = LoggerFactory.getLogger(RustGenerator.class);
  private final boolean useDecimal;
  private final boolean allFieldsIsRequired;
  private final boolean allParamsIsRequired;
  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";
  protected String packageName = "swagger";
  protected String packageVersion;
  protected String apiDocPath = "docs/";
  protected String modelDocPath = "docs/";
  protected String apiFolder = "src/apis";
  protected String testsFolder = "src/tests";
  protected String modelFolder= "src/models";
  protected HashMap<String, String> arrayModels;
  protected Transliterator transliterator;

  /**
   * Configures the type of generator.
   * 
   * @return  the CodegenType for this generator
   * @see     io.swagger.codegen.v3.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -l flag.
   * 
   * @return the friendly name for the generator
   */
  public String getName() {
    return "rust";
  }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   * 
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a rust client library.";
  }

  public RustGenerator() {
    super();
    //String rules = "Any-Latin; Latin-ASCII";
    //transliterator = Transliterator.createFromRules("transliterator", rules, Transliterator.FORWARD);
    transliterator = Transliterator.getInstance("Any-Latin");

    allFieldsIsRequired = Optional.ofNullable(System.getProperty("allFieldsIsRequired"))
            .map(v -> v.equals("true"))
            .orElse(false);
    allParamsIsRequired = Optional.ofNullable(System.getProperty("allParamsIsRequired"))
            .map(v -> v.equals("true"))
            .orElse(false);
    useDecimal = Optional.ofNullable(System.getProperty("useDecimal"))
            .map(v -> v.equals("true"))
            .orElse(true);
    // set the output folder here
    outputFolder = "generated-code/rust";

    packageVersion = "0.0.0";

    arrayModels = new HashMap<>();

    /**
     * Models.  You can write model files using the modelTemplateFiles map.
     * if you want to create one template for file, you can do so here.
     * for multiple files for model, just put another entry in the `modelTemplateFiles` with
     * a different extension
     */
    modelTemplateFiles.put(
            "model.mustache", // the template to use
            ".rs");       // the extension for each file to write

    /**
     * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple files per
     * class
     */
    apiTemplateFiles.put(
            "api.mustache",   // the template to use
            ".rs");       // the extension for each file to write

    apiTemplateFiles.put(
            "api2nd.mustache",   // the template to use
            "2nd.rs");       // the extension for each file to write

    modelDocTemplateFiles.put("model_doc.mustache", ".md");
    apiDocTemplateFiles.put("api_doc.mustache", ".md");

    /**
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "rust";

    /**
     * Api Package.  Optional, if needed, this can be used in templates
     */
    apiPackage = "io.swagger.client.api";

    /**
     * Model Package.  Optional, if needed, this can be used in templates
     */
    modelPackage = "io.swagger.client.model";

    /**
     * Reserved words.  Override this with reserved words specific to your language
     */
    setReservedWordsLowerCase(
            Arrays.asList(
                    "abstract", "alignof", "as", "become", "box",
                    "break", "const", "continue", "crate", "do",
                    "else", "enum", "extern", "false", "final",
                    "fn", "for", "if", "impl", "in",
                    "let", "loop", "macro", "match", "mod",
                    "move", "mut", "offsetof", "override", "priv",
                    "proc", "pub", "pure", "ref", "return",
                    "Self", "self", "sizeof", "static", "struct",
                    "super", "trait", "true", "type", "typeof",
                    "unsafe", "unsized", "use", "virtual", "where",
                    "while", "yield", "Decimal"
            )
    );


    /**
     * Additional Properties.  These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);

    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
    supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
    supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
    supportingFiles.add(new SupportingFile("configuration.mustache", apiFolder, "configuration.rs"));
    supportingFiles.add(new SupportingFile(".travis.yml", "", ".travis.yml"));

    supportingFiles.add(new SupportingFile("client.mustache", apiFolder, "client.rs"));
    supportingFiles.add(new SupportingFile("api_mod.mustache", apiFolder, "mod.rs"));
    supportingFiles.add(new SupportingFile("api_test_mod.mustache", testsFolder, "mod.rs"));
    supportingFiles.add(new SupportingFile("model_mod.mustache", modelFolder, "mod.rs"));
    supportingFiles.add(new SupportingFile("lib.mustache", "src", "lib.rs"));
    supportingFiles.add(new SupportingFile("date_serializer.rs", "src", "date_serializer.rs"));
    supportingFiles.add(new SupportingFile("date_serializer_opt.rs", "src", "date_serializer_opt.rs"));
    supportingFiles.add(new SupportingFile("datetime_serializer.rs", "src", "datetime_serializer.rs"));
    supportingFiles.add(new SupportingFile("serialize_quoted_numbers.rs", "src", "serialize_quoted_numbers.rs"));
    supportingFiles.add(new SupportingFile("serialize_quoted_numbers_opt.rs", "src", "serialize_quoted_numbers_opt.rs"));
    supportingFiles.add(new SupportingFile("Cargo.mustache", "", "Cargo.toml"));
    additionalProperties.put("apiTestPath", "tests");
    apiTestTemplateFiles.put("api_test.mustache", ".rs");
    //supportingFiles.add(new SupportingFile("api_test.mustache", "src", "api_test.rs"));
    defaultIncludes = new HashSet<String>(
            Arrays.asList(
                    "map",
                    "array")
    );

    /**
     * Language Specific Primitives.  These types will not trigger imports by
     * the client generator
     */
    languageSpecificPrimitives = new HashSet<String>(
            Arrays.asList(
                    "i8", "i16", "i32", "i64", "i128",
                    "u8", "u16", "u32", "u64", "u128",
                    "f32", "f64", "str", "String", "Decimal",
                    "char", "bool", "Vec<u8>", "File")
    );

    instantiationTypes.clear();

    typeMapping.clear();
    typeMapping.put("integer", "i32");
    typeMapping.put("int32", "i32");
    typeMapping.put("long", "i64");
    typeMapping.put("int64", "i64");
    if(useDecimal) {
      typeMapping.put("number", "Decimal");
      typeMapping.put("float", "Decimal");
      typeMapping.put("double", "Decimal");
    } else {
      typeMapping.put("number", "f32");
      typeMapping.put("float", "f32");
      typeMapping.put("double", "f64");
    }
    typeMapping.put("boolean", "bool");
    typeMapping.put("string", "String");
    typeMapping.put("UUID", "String");
    typeMapping.put("date", "String");
    typeMapping.put("DateTime", "String");
    typeMapping.put("password", "String");
    typeMapping.put("file", "File");
    typeMapping.put("binary", "Vec<u8>");
    typeMapping.put("ByteArray", "String");
    typeMapping.put("object", "Value");

    cliOptions.clear();
    cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "Rust package name (convention: lowercase).")
            .defaultValue("swagger"));
    cliOptions.add(new CliOption(CodegenConstants.PACKAGE_VERSION, "Rust package version.")
            .defaultValue("1.0.0"));
    cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC)
            .defaultValue(Boolean.TRUE.toString()));
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   * 
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "r" + name;  // add an underscore to the name
  }

  public Map<String, String> createMapping(String key, String value) {
    Map<String, String> customImport = new HashMap<>();
    customImport.put(key, value);
    return customImport;
  }

  @Override
  public String apiTestFileFolder() {
    return (outputFolder + "/" + testsFolder).replace('/', File.separatorChar);
  }


  private String findEnumName(int truncateIdx, Object value) {
    if (value == null) {
      return "null";
    } else {
      String enumName;
      if (truncateIdx == 0) {
        enumName = value.toString();
      } else {
        enumName = value.toString().substring(truncateIdx);
        if ("".equals(enumName)) {
          enumName = value.toString();
        }
      }

      return enumName;
    }
  }

  @SuppressWarnings("static-method")
  public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
    if (co.path.startsWith("/")) {
      co.path = co.path.substring(1);
    }

    super.addOperationToGroup(tag, co.path, operation, co, operations);
  }

  @Override
  public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
    Map<String, Object> allProcessedModels = super.postProcessAllModels(objs);
    return allProcessedModels;
  }

  @Override
  public Map<String, Object> postProcessModels(Map<String, Object> objs) {
    // remove model imports to avoid error
    List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
    final String prefix = modelPackage();
    Iterator<Map<String, String>> iterator = imports.iterator();
    while (iterator.hasNext()) {
      String _import = iterator.next().get("import");
      if (_import.startsWith(prefix))
        iterator.remove();
    }

    boolean addedTimeImport = false;
    List<Map<String, Object>> models = (List<Map<String, Object>>) objs.get("models");
    ArrayList<Map<String, Object>> modelsToRemove = new ArrayList<>();
    for (Map<String, Object> m : models) {
      Object v = m.get("model");
      if (v instanceof CodegenModel) {
        CodegenModel model = (CodegenModel) v;
        ArrayList<CodegenProperty> requiredFields = new ArrayList<>();

        //Here we fix enum name
        boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
        model.setName(toModelName(model.getName()));

        boolean isArray = getBooleanValue(model, IS_ARRAY_MODEL_EXT_NAME);

        if (isArray) {
          //System.out.printf("Model %s is an array!! Model: %s\n", model.getName(), new Gson().toJson(model));
          arrayModels.put(model.getClassname(), model.getArrayModelType());
          model.setArrayModelType(toModelName(model.getArrayModelType()));
          modelsToRemove.add(m);
          continue;
        }

        for (CodegenProperty param : model.vars) {
          if ((allFieldsIsRequired && !param.getIsNullable()) || param.required) {
            param.required = true;
            requiredFields.add(param);
          }
          if(param.getIsString() && param.getDataFormat()!=null) {
            String format = param.getDataFormat();
            String extension = null;
            switch (format) {
              case "float":
                extension = IS_FLOAT_EXT_NAME;
                break;
              case "double":
                extension = IS_DOUBLE_EXT_NAME;
                break;
              case "int32":
                extension = IS_INTEGER_EXT_NAME;
                break;
              case "int64":
                extension = IS_LONG_EXT_NAME;
                break;
            }
            if(extension!=null) {
              param.vendorExtensions.put(extension, true);
              param.vendorExtensions.put("x-is-numeric", true); //Then check it in template as isString and isNumeric same time
            }
          }
          if (param.baseType != null) {
            if (!addedTimeImport && param.baseType.equals("DateTime")) {
              imports.add(createMapping("use", "chrono::{NaiveDateTime, DateTime, Utc, Local, TimeZone, SecondsFormat, Utc}"));
              addedTimeImport = true;
            } else {
              imports.add(createMapping("use", param.baseType));
            }
          }
        }
        System.out.printf("Set %d required fields for model %s%n", requiredFields.size(), model.getName());
        model.setRequiredVars(requiredFields);
      }
    }
    //models.removeAll(modelsToRemove);
    // recursively add import for mapping one type to multiple imports
    List<Map<String, String>> recursiveImports = (List<Map<String, String>>) objs.get("imports");
    if (recursiveImports == null)
      return objs;

    ListIterator<Map<String, String>> listIterator = imports.listIterator();
    while (listIterator.hasNext()) {
      String _import = listIterator.next().get("import");
      // if the import package happens to be found in the importMapping (key)
      // add the corresponding import package to the list
      if (importMapping.containsKey(_import)) {
        listIterator.add(createMapping("import", importMapping.get(_import)));
      }
    }

    return postProcessModelsEnum(objs);
  }


  /**
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    return (outputFolder + File.separator + apiFolder).replace("/", File.separator);
  }

  public String modelFileFolder() {
    return (outputFolder + File.separator + modelFolder).replace("/", File.separator);
  }

  String transliterate(String inputText) {
    if (inputText != null) {
      return transliterator.transliterate(inputText);
    } else {
      return inputText;
    }
  }

  @Override
  public String toVarName(String name) {
    // replace - with _ e.g. created-at => created_at
    name = transliterate(name);
    name = sanitizeName(name.replaceAll("-", "_"));


    // snake_case, e.g. PetId => pet_id
    name = underscore(name).replace("__", "_");

    // for reserved word or word starting with number, append _
    if (isReservedWord(name))
      name = escapeReservedWord(name);

    // for reserved word or word starting with number, append _
    if (name.matches("^\\d.*"))
      name = "var_" + name;

    return name;
  }

  @Override
  public String toParamName(String name) {
    return toVarName(name);
  }

  public static String pascalize(String word) {
    String res = DefaultCodegenConfig.camelize(word);
    res = res.substring(0, 1).toUpperCase() + res.substring(1);
    return res;
  }

  @Override
  public String toModelName(String name) {
    // camelize the model name
    // phone_number => PhoneNumber
    return pascalize(toModelFilename(name));
  }

  @Override
  public String toModelFilename(String name) {
    name = transliterate(name);
    if (!StringUtils.isEmpty(modelNamePrefix)) {
      name = modelNamePrefix + "_" + name;
    }

    if (!StringUtils.isEmpty(modelNameSuffix)) {
      name = name + "_" + modelNameSuffix;
    }

    name = sanitizeName(name);

    // model name cannot use reserved keyword, e.g. return
    if (isReservedWord(name)) {
      LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + ("model_" + name));
      name = "model_" + name; // e.g. return => ModelReturn (after camelize)
    }

    // model name starts with number
    if (name.matches("^\\d.*")) {
      LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + ("model_" + name));
      name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
    }

    return underscore(name);
  }

  @Override
  public String toApiTestFilename(String name) {
    // replace - with _ e.g. created-at => created_at
    name = transliterate(name);
    name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

    // e.g. PetApi.rs => pet_api.rs
    return underscore(name) + "_api_test";
  }

  @Override
  public String toApiFilename(String name) {
    name = transliterate(name);
    // replace - with _ e.g. created-at => created_at
    name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

    // e.g. PetApi.rs => pet_api.rs
    String underscored = underscore(name);
    if (underscored.endsWith("api")) {
      return underscored;
    } else{
      return underscored + "_api";
    }
  }
  @Override
  public String toApiName(String name) {
    name = transliterate(name);
    // replace - with _ e.g. created-at => created_at
    name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

    // e.g. PetApi.rs => pet_api.rs
    String pascalized = pascalize(name);
    if (pascalized.endsWith("Api")) {
      return pascalized;
    } else{
      return pascalized + "Api";
    }
  }

  @Override
  public String apiDocFileFolder() {
    return (outputFolder + "/" + apiDocPath).replace('/', File.separatorChar);
  }

  @Override
  public String modelDocFileFolder() {
    return (outputFolder + "/" + modelDocPath).replace('/', File.separatorChar);
  }

  @Override
  public String toModelDocFilename(String name) {
    return toModelName(name);
  }

  @Override
  public String toApiDocFilename(String name) {
    return toApiName(name);
  }

  public String getSchemaType(Schema schema) {
    String schemaType = super.getSchemaType(schema);
    if (schema.get$ref() != null) {
      Schema refSchema = OpenAPIUtil.getSchemaFromName(schemaType, this.openAPI);
      if (refSchema != null && !this.isObjectSchema(refSchema)) {
        schemaType = super.getSchemaType(refSchema);
      }
    }

    String type;
    if (this.typeMapping.containsKey(schemaType)) {
      type = (String)this.typeMapping.get(schemaType);
      if (this.languageSpecificPrimitives.contains(type)) {
        return type;
      }
    } else {
      type = schemaType;
    }

    return type;
  }

  @Override
  public String getTypeDeclaration(Schema schema) {
    Schema inner;
    String schemaType = this.getSchemaType(schema);

    if (schema instanceof ArraySchema) {
      ArraySchema arraySchema = (ArraySchema)schema;
      inner = arraySchema.getItems();
      return "Vec<" + this.getTypeDeclaration(inner) + ">";
    } else if (schema instanceof MapSchema) {
      MapSchema mapSchema = (MapSchema)schema;
      inner = (Schema)mapSchema.getAdditionalProperties();
      return "::std::collections::HashMap<String, " + this.getTypeDeclaration(inner) + ">";
    } else if (schema instanceof StringSchema) {
      String format = schema.getFormat();
      if(format != null && typeMapping.containsKey(format)) {
        return typeMapping.get(format);
      } else {
        return "String";
      }
    } else if (schema instanceof NumberSchema || schema instanceof IntegerSchema) {
      String format = schema.getFormat();
      String result;
      if(format != null) {
        result = typeMapping.get(format);
      } else if (schema instanceof IntegerSchema) {
        result = "i32";
      } else if (schema instanceof NumberSchema) {
        if(useDecimal) {
          result = "Decimal";
        } else {
          result = "f64";
        }
      } else {
        System.out.println("WARNING: unknown schema "+schema.getType());
        result = "i64";
      }
      return result;
    } else if (schema instanceof BooleanSchema) {
      return "bool";
    } else if (schema instanceof DateTimeSchema) {
      return "DateTime<Utc>";
    } else if (schema instanceof DateSchema) {
      schema.setFormat("date");
      return "NaiveDate";
    } else {
      if (this.typeMapping.containsKey(schemaType)) {
        return (String)this.typeMapping.get(schemaType);
      } else if (schema.get$ref() != null) {
        String[] refh = schema.get$ref().split("/");
        String modelName = this.toModelName(refh[refh.length-1]);

        if (schemaType.equals("array") && arrayModels.containsKey(modelName)) {
          String innerType = arrayModels.get(modelName);
          if (!this.typeMapping.containsKey(innerType)) {
            innerType = toModelName(innerType);
          }
          System.out.printf("!!! Got array schema %s with inner type %s\n", modelName, innerType);
          return "Vec<" + innerType + ">";
        } else {
          return String.format("%s", modelName);
        }
      } else if (this.typeMapping.containsValue(schemaType)) {
        return schemaType;
      } else {
        String modelName = this.toModelName(schemaType);

        System.out.println(
          "WARNING: could not resolve given type (schema type: " + schemaType + ", model name: " + modelName + "). The generated code is probably faulty. Check the schema!"
        );

        return this.languageSpecificPrimitives.contains(schemaType) ? ""+schemaType
                : "" + modelName;
      }
    }
  }

  @Override
  public String toOperationId(String operationId) {
    String sanitizedOperationId = sanitizeName(operationId);

    // method name cannot use reserved keyword, e.g. return
    if (isReservedWord(sanitizedOperationId)) {
      LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + underscore("call_" + operationId));
      sanitizedOperationId = "call_" + sanitizedOperationId;
    }

    return underscore(sanitizedOperationId).replace("__", "_");
  }


  @Override
  public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
    @SuppressWarnings("unchecked")
    Map<String, Object> objectMap = (Map<String, Object>) objs.get("operations");
    @SuppressWarnings("unchecked")
    List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
    for (CodegenOperation operation : operations) {
      List<CodegenParameter> requiredParams = new ArrayList<>();
      operation.httpMethod = pascalize(operation.httpMethod.toUpperCase());
      operation.baseName = pascalize(operation.baseName.toUpperCase());
      for (CodegenParameter param : operation.allParams) {
        if ((allParamsIsRequired && !param.getIsNullable())) {
          param.required = true;
        }
      }
      for (CodegenParameter param : operation.queryParams) {
        if ((allParamsIsRequired && !param.getIsNullable())) {
          param.required = true;
        }
      }
      for (CodegenParameter param : operation.bodyParams) {
        if ((allParamsIsRequired && !param.getIsNullable())) {
          param.required = true;
        }
      }
      operation.requiredParams = requiredParams;

    }

    return objs;
  }
  @Override
  public void postProcessParameter(CodegenParameter parameter) {
    super.postProcessParameter(parameter);
    String dataType = parameter.getDataType();
    if(dataType != null && dataType.equals("DateTime")) {
      parameter.dataFormat = "datetime";
      if(parameter.example == null) parameter.example = "2019-03-19T18:38:33.131642+03:00";
    }
    if(parameter.example != null) System.out.printf("Example for %s is %s\n", parameter.paramName, parameter.example);
    if(parameter.testExample != null) System.out.printf("testExample for %s is %s\n", parameter.paramName, parameter.testExample);
  }

  @Override
  protected boolean needToImport(String type) {
    return !defaultIncludes.contains(type)
            && !languageSpecificPrimitives.contains(type);
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public void setPackageVersion(String packageVersion) {
    this.packageVersion = packageVersion;
  }

  @Override
  public String escapeQuotationMark(String input) {
    // remove " to avoid code injection
    return input.replace("\"", "");
  }

  @Override
  public String escapeUnsafeCharacters(String input) {
    return input.replace("*/", "*_/").replace("/*", "/_*");
  }


  @Override
  public String toEnumValue(String value, String datatype) {
    if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
      return value;
    } else {
      return escapeText(value);
    }
  }

  @Override
  public String toEnumDefaultValue(String value, String datatype) {
    return datatype + "_" + value;
  }

  @Override
  public String toEnumVarName(String name, String datatype) {
    if (name.length() == 0) {
      return "EMPTY";
    }

    // number
    if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
      String varName = name;
      varName = varName.replaceAll("-", "MINUS_");
      varName = varName.replaceAll("\\+", "PLUS_");
      varName = varName.replaceAll("\\.", "_DOT_");
      return varName;
    }

    // for symbol, e.g. $, #
    if (getSymbolName(name) != null) {
      return getSymbolName(name).toUpperCase();
    }

    // string
    String enumName = sanitizeName(underscore(name).toUpperCase());
    enumName = enumName.replaceFirst("^_", "");
    enumName = enumName.replaceFirst("_$", "");

    if (isReservedWord(enumName) || enumName.matches("\\d.*")) { // reserved word or starts with number
      return escapeReservedWord(enumName);
    } else {
      return enumName;
    }
  }

  @Override
  public String toEnumName(CodegenProperty property) {
    String enumName = underscore(toModelName(property.name)).toUpperCase();
    // remove [] for array or map of enum
    enumName = enumName.replace("[]", "");

    if (enumName.matches("\\d.*")) { // starts with number
      return "_" + enumName;
    } else {
      return enumName;
    }
  }




  @Override
  public String getArgumentsLocation() {
    return null;
  }

  @Override
  protected String getTemplateDir() {
    return templateDir;
  }

  @Override
  public String getDefaultTemplateDir() {
    return templateDir;
  }
}
