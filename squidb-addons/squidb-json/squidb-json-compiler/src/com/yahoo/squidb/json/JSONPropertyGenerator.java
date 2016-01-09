/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.visitors.ImportGatheringTypeNameVisitor;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicStringPropertyGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.VariableElement;

public class JSONPropertyGenerator extends BasicStringPropertyGenerator {

    private static final DeclaredTypeName PARAMETERIZED_TYPE_BUILDER = new DeclaredTypeName(
            "com.yahoo.squidb.json.ParameterizedTypeBuilder");
    private static final DeclaredTypeName SQUIDB_JSON_SUPPORT = new DeclaredTypeName(
            "com.yahoo.squidb.json.SquidbJSONSupport");

    protected final DeclaredTypeName fieldType;

    public JSONPropertyGenerator(ModelSpec<?> modelSpec, VariableElement field, DeclaredTypeName fieldType,
            AptUtils utils) {
        super(modelSpec, field, utils);
        this.fieldType = fieldType;
    }

    @Override
    protected void registerAdditionalImports(Set<DeclaredTypeName> imports) {
        super.registerAdditionalImports(imports);
        imports.add(SQUIDB_JSON_SUPPORT);
        if (!AptUtils.isEmpty(fieldType.getTypeArgs())) {
            imports.add(PARAMETERIZED_TYPE_BUILDER);
        }
        fieldType.accept(new ImportGatheringTypeNameVisitor(), imports);
    }

    @Override
    protected DeclaredTypeName getTypeForGetAndSet() {
        return fieldType;
    }

    @Override
    protected void writeGetterBody(JavaFileWriter writer) throws IOException {
        Expression typeExpression = getTypeExpression(fieldType);

        writer.writeStatement(Expressions.staticMethod(SQUIDB_JSON_SUPPORT, "getObjectValue",
                "this", propertyName, typeExpression).returnExpr());
    }

    @Override
    protected void writeSetterBody(JavaFileWriter writer, String argName) throws IOException {
        writer.writeStatement(Expressions.staticMethod(SQUIDB_JSON_SUPPORT, "setObjectProperty",
                "this", propertyName, argName));
        writer.writeStringStatement("return this");
    }

    private Expression getTypeExpression(DeclaredTypeName fieldType) {
        List<? extends TypeName> typeArgs = fieldType.getTypeArgs();
        if (AptUtils.isEmpty(typeArgs)) {
            return Expressions.classObject(fieldType);
        } else {
            List<Expression> parameterizedTypeBuilderArgs = new ArrayList<Expression>();
            parameterizedTypeBuilderArgs.add(Expressions.classObject(fieldType));
            for (TypeName typeArg : typeArgs) {
                // The cast to DeclaredTypeName is safe because we recursively check all type args before constructing
                // an instance of this property generator
                parameterizedTypeBuilderArgs.add(getTypeExpression((DeclaredTypeName) typeArg));
            }
            return Expressions.staticMethod(PARAMETERIZED_TYPE_BUILDER, "build", parameterizedTypeBuilderArgs);
        }
    }

}
