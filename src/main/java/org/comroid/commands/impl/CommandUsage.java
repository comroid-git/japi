package org.comroid.commands.impl;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.comroid.annotations.Default;
import org.comroid.api.func.util.Optionals;
import org.comroid.api.func.util.Streams;
import org.comroid.api.text.StringMode;
import org.comroid.commands.model.CommandResponseHandler;
import org.comroid.commands.node.Callable;
import org.comroid.commands.node.Node;
import org.comroid.commands.node.Parameter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

@Value
@Builder
public class CommandUsage {
    CommandManager manager;
    String[]       fullCommand;
    @NotNull CommandResponseHandler source;
    @NotNull Callable               baseNode;
    @Default                       Stack<Callable>        stackTrace      = new Stack<>();
    @Default                       Stack<Parameter>       paramTrace      = new Stack<>();
    @Default @Singular("argument") Map<Parameter, String> argumentStrings = new ConcurrentHashMap<>();
    @Default @Singular("context")  Set<Object>            context;

    public void advanceFull() {
        stackTrace.clear();
        paramTrace.clear();

        stackTrace.push(baseNode);

        var helper = new Object() {
            Node     node             = baseNode;
            Callable lastCallable     = null;
            int      fullCommandIndex = 1;

            boolean findNext() {
                if (fullCommandIndex >= fullCommand.length) return false;
                var part = fullCommand[fullCommandIndex];

                callable:
                if (node instanceof Callable callable) {
                    var result = callable.nodes().filter(node -> Objects.equals(part, node.getName())).findAny();
                    if (result.isEmpty()) break callable;
                    this.node         = result.get();
                    this.lastCallable = result.flatMap(Optionals.cast(Callable.class)).orElse(callable);
                    this.fullCommandIndex += 1;
                    return true;
                }

                if (lastCallable == null) return false;

                var params = lastCallable.nodes().flatMap(Streams.cast(Parameter.class)).toList();
                node = params.getFirst();
                if (node instanceof Parameter param) {
                    // store argString
                    var argString = new StringBuilder(fullCommand[fullCommandIndex]);
                    switch (param.getAttribute().stringMode()) {
                        case NORMAL, GREEDY -> {
                            if (!argString.toString().startsWith("\"")) break;
                            // immediately consume greedy argument
                            argString = new StringBuilder(argString.substring(1));
                            String next;
                            do {
                                argString.append(next = fullCommand[++fullCommandIndex]);
                            } while (!next.endsWith("\"") && fullCommandIndex + 1 < fullCommand.length);
                            if (next.endsWith("\"")) argString.deleteCharAt(argString.length() - 1);
                        }
                    }
                    if (!argString.toString().isBlank()) argumentStrings.put(param, argString.toString());

                    // advance parameter if possible
                    var nextIndex = params.indexOf(param) + 1;
                    if (nextIndex == -1) return false;
                    if (nextIndex >= params.size() && param.getAttribute()
                                                              .stringMode() == StringMode.SINGLE_WORD || nextIndex + 1 >= params.size())
                        return false;
                    this.node = params.get(nextIndex + 1);
                    this.fullCommandIndex += 1;
                    return true;
                }

                return false;
            }

            void commit() {
                if (node instanceof Callable callable) stackTrace.push(callable);
                if (node instanceof Parameter parameter) paramTrace.push(parameter);
            }
        };

        while (helper.findNext()) helper.commit();
    }
}
