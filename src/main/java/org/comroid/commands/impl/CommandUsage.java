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
import java.util.stream.Stream;

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
                if (!(node instanceof Parameter)) node = params.getFirst();

                if (node instanceof Parameter param) {
                    // store argString
                    var argString = new StringBuilder(fullCommand[fullCommandIndex]);
                    if (Objects.requireNonNull(param.getAttribute().stringMode()) == StringMode.GREEDY) {
                        // immediately consume greedy argument
                        if (!argString.isEmpty() && argString.charAt(0) == '"') argString.deleteCharAt(0);
                        String read = "";
                        while (!read.endsWith("\"") && fullCommandIndex + 1 < fullCommand.length) argString.append(' ')
                                .append(read = fullCommand[++fullCommandIndex]);
                        if (read.endsWith("\"")) argString.deleteCharAt(argString.length() - 1);
                    }
                    if (!argString.toString().isBlank()) argumentStrings.put(param, argString.toString());

                    // advance parameter if possible
                    var nextIndex = params.indexOf(param) + 1;
                    if (nextIndex >= params.size() || params.get(nextIndex)
                                                              .getAttribute()
                                                              .stringMode() == StringMode.SINGLE_WORD) return false;

                    this.node = params.get(nextIndex);
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

    public <T> Stream<T> fromContext(Class<T> type) {
        return context.stream().flatMap(Streams.cast(type));
    }
}
