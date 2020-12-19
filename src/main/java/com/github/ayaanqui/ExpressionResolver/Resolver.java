package com.github.ayaanqui.ExpressionResolver;

import java.lang.Math;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

import com.github.ayaanqui.ExpressionResolver.objects.Response;
import com.github.ayaanqui.ExpressionResolver.util.ConvertConstants;
import com.github.ayaanqui.ExpressionResolver.util.EvaluateParentheses;
import com.github.ayaanqui.ExpressionResolver.util.MathFunctions;

public class Resolver {
    private final char[] operatorList = { '+', '-', '*', '/', '^', '(', ')', '<', '=' };

    private String userInput;
    private LinkedList<String> formattedUserInput;
    private Double lastResult = null;
    private Map<String, Double> variableMap;
    private Map<String, Function<Double[], Double>> functionList;

    public Resolver() {
        formattedUserInput = new LinkedList<>();
        variableMap = new HashMap<>(15);
        functionList = new HashMap<>(15);

        // Define constants
        variableMap.put("pi", Math.PI);
        variableMap.put("e", Math.E);
        variableMap.put("tau", 2 * Math.PI);

        functionList.put("sqrt", args -> Math.sqrt(args[0]));
        functionList.put("sin", args -> Math.sin(args[0]));
        functionList.put("cos", args -> Math.cos(args[0]));
        functionList.put("tan", args -> Math.tan(args[0]));
        functionList.put("ln", args -> Math.log(args[0]));
        functionList.put("deg", args -> Math.toDegrees(args[0]));
        functionList.put("abs", args -> Math.abs(args[0]));
        functionList.put("exp", args -> Math.exp(args[0]));
        functionList.put("fact", args -> {
            double factorial = 1;
            for (int i = args[0].intValue(); i > 1; i--)
                factorial *= i;
            return factorial;
        });
        functionList.put("arcsin", args -> Math.asin(args[0]));
        functionList.put("arccos", args -> Math.acos(args[0]));
        functionList.put("arctan", args -> Math.atan(args[0]));
    }

    public Resolver setExpression(String uInp) {
        this.userInput = uInp;
        formattedUserInput = new LinkedList<>();
        return this;
    }

    /**
     * This method allows to skip the parsing of the string if the input has already
     * been formatted. <br />
     * Example subList:
     * 
     * <pre>
     * <code>LinkedList<String>: ["1", "+", "sin" + "(" + "pi" + ")"]</code>
     * </pre>
     * 
     * @param subList LinkedList<String> with a pre formatted input
     * @return Returns a Resolver object
     */
    public Resolver expressionList(LinkedList<String> subList) {
        this.formattedUserInput = subList;
        return this;
    }

    public String getUserInput() {
        return userInput;
    }

    public double getLastResult() {
        return lastResult;
    }

    /**
     * Given positions for prefix, operator, and postfix, format negatives. Given
     * formattedList [.., "x", "-", "num", ..] returns [.., "x", "+", "-num", ..],
     * where x = ")" or a number. For everything else removes "-" and concatinates
     * "-" to post.
     *
     * @param formattedList
     * @param pre           Left hand side opperand
     * @param op            Operator. Assumes the value at index is "-"
     * @param post          Right hand side opperand
     * @return if processed correctly true, else false
     */
    private boolean handleNegative(LinkedList<String> formattedList, int pre, int op, int post) {
        if (op >= formattedList.size() || post >= formattedList.size())
            return false;

        if (pre >= 0 && Character.isDigit(formattedList.get(pre).charAt(0))
                && Character.isDigit(formattedList.get(post).charAt(0)))
            return true;

        // When pre >= 0 and the value at pre == ")"
        if (pre >= 0 && formattedList.get(pre).equals(")")) {
            formattedList.set(op, "+");
            formattedList.add(op + 1, "-1");
            formattedList.add(op + 2, "*");
            return true;
        }

        // Concatinates op with post, then removes op
        if (op >= 0 && post >= 1 && post < formattedList.size()) {
            if (Character.isDigit(formattedList.get(post).charAt(0))) {
                formattedList.set(post, '-' + formattedList.get(post));
                formattedList.remove(op);
                return true;
            }
            // If post is not a number it could be an opening parethesis or a function
            // In this case add [.., "-1", "*", ..] in front
            if (pre >= 0) {
                formattedList.set(op, "+");
                formattedList.add(op + 1, "-1");
                formattedList.add(op + 2, "*");
            } else {
                formattedList.set(op, "-1");
                formattedList.add(op + 1, "*");
            }
            return true;
        }
        return false;
    }

    /**
     * Converts "<" to the last item from userHistory, and handles elements with
     * "-".
     *
     * @param formattedList
     */
    private void operatorFormatting(LinkedList<String> formattedList) {
        for (int i = 0; i < formattedList.size(); i++) {
            String item = formattedList.get(i);

            if (item.equals("-")) {
                handleNegative(formattedList, i - 1, i, i + 1);
            } else if (item.equals("<")) { // get the the previous answer
                if (lastResult != null)
                    formattedList.set(i, lastResult.toString());
                else
                    formattedList.set(i, "0");
            }
        }
    }

    public LinkedList<String> formatUserInput() {
        // Trim whitespaces and $ signs
        userInput = userInput.replaceAll("\\s", "");
        userInput = userInput.replace("$", "");

        LinkedList<String> formattedList = new LinkedList<>();
        int start = 0;
        for (int i = 0; i < userInput.length(); i++) {
            for (char operator : operatorList) {
                if (operator == userInput.charAt(i)) {
                    // Content before operator
                    String prefix = userInput.substring(start, i);
                    if (prefix.length() > 0)
                        formattedList.add(prefix);
                    formattedList.add(Character.toString(operator));
                    start = i + 1;
                    break;
                }
            }
        }

        if (userInput.equals(""))
            return formattedList;

        String remainder = userInput.substring(start);
        if (!remainder.equals(""))
            formattedList.add(remainder);

        // Remove "+" if it is at the begining of the list
        if (formattedList.get(0).equals("+"))
            formattedList.remove(0);

        ConvertConstants cOb = new ConvertConstants(formattedList, variableMap);
        cOb.convert();

        operatorFormatting(formattedList);

        return formattedList;
    }

    private Response catchNumberException(String elem) {
        double result;
        try {
            result = Double.parseDouble(elem); // value of i-1
        } catch (NumberFormatException e) {
            return Response.getError(new String[] { "Operator requires two numbers" });
        }
        return Response.getSuccess(result);
    }

    public Response condenseExpression(char operator, int i) {
        // Check to see if (i-1) and (i-1) are within the bounds of formattedUserInput
        if (i - 1 < 0 || i + 1 >= formattedUserInput.size()) {
            return Response.getError(new String[] { "Operator requires two numbers" });
        }

        String lhs = formattedUserInput.get(i - 1);
        Response x = catchNumberException(lhs);
        Response y = catchNumberException(formattedUserInput.get(i + 1));

        // Handle variables
        if (operator == '=') {
            // x cannot start with a number
            if (!x.success) {
                x.success = true;
                x.errors = new String[0];
            } else {
                x.success = false;
                x.errors = new String[] { "Variable names cannot start with a number",
                        "Variables cannot be reassigned" };
            }

            // y must be a number
            if (!y.success) {
                y.success = false;
                y.errors = new String[] { "Variable value must be a number" };
            }
        }

        if (!x.success)
            return x;
        if (!y.success)
            return y;

        double output = 0.0;
        if (operator == '^')
            output = Math.pow(x.result, y.result);
        else if (operator == '/')
            output = x.result / y.result;
        else if (operator == '*')
            output = x.result * y.result;
        else if (operator == '-')
            output = x.result - y.result;
        else if (operator == '+')
            output = x.result + y.result;
        else if (operator == '=') {
            this.variableMap.put(lhs, y.result);
            output = y.result;
        }

        return Response.getSuccess(output);
    }

    public Response solveExpression() {
        if (formattedUserInput.isEmpty()) {
            this.formattedUserInput = formatUserInput();
        }

        if (formattedUserInput.isEmpty())
            return Response.getError(new String[] { "Input cannot be left blank" });

        Response evalFunctionsResponse = new MathFunctions(formattedUserInput, functionList).evaluateFunctions();
        if (!evalFunctionsResponse.success)
            return evalFunctionsResponse;

        Response res = new Response();
        // Perform parentheses before everything
        for (int i = 0; i < formattedUserInput.size() - 1; i++) {
            if (formattedUserInput.get(i).equals("(")) {
                res = EvaluateParentheses.condense(formattedUserInput, i);
                if (!res.success)
                    return res;
                i--;
            }
        }

        final char[][] orderOfOperations = new char[][] { { '^' }, { '*', '/' }, { '+', '-' }, { '=' } };
        for (char[] operators : orderOfOperations) {
            for (int i = 1; i < formattedUserInput.size(); i++) {
                char inputOp = formattedUserInput.get(i).charAt(0);

                for (char op : operators) {
                    if (op == inputOp) {
                        res = condenseExpression(op, i);
                        if (!res.success)
                            return res;

                        if (op == '/' && Double.isInfinite(res.result))
                            return Response.getError(new String[] { "Could not divide by zero" });

                        formattedUserInput.remove(i + 1); // Remove rhs operand
                        formattedUserInput.remove(i); // Remove operator
                        formattedUserInput.set(i - 1, Double.toString(res.result));
                        i--;
                    }
                }
            }
        }

        if (formattedUserInput.size() == 1) {
            double expression;
            try {
                expression = Double.parseDouble(formattedUserInput.get(0));
            } catch (Exception e) {
                return Response.getError(new String[] { "Not a number", "Error parsing input" });
            }
            lastResult = expression;
            return Response.getSuccess(expression);
        } else {
            return Response.getError(new String[] { "Could not resolve expression" });
        }
    }
}