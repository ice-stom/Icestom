package io.gitlab.icestom.icestom.command;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.event.Result;
import io.gitlab.icestom.icestom.event.lua.LuaEvent;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.minestom.server.command.builder.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(EventCommand.class);

    public EventCommand() {
        super("event");

        addSubcommand(new EventTestCommand());
    }

    public static class EventTestCommand extends Command {
        public EventTestCommand() {
            super("test");

            setDefaultExecutor((commandSender, commandContext) -> {
                if (!(commandSender instanceof IceStomPlayer player)) return;

                try {
                    final LuaEvent<EventParticipant> luaEvent = new LuaEvent<>(
                            "test.luau",
                            LuauCompiler.DEFAULT.compile("""
                            --!nocheck
                            local icestom = require("@icestom")
                            
                            local params = ...
                            local track = params.track
                            
                            print("Selected track: " .. track)
                            
                            local event: icestom.Event = function(event, results)
                                print("making the practice stage!")
                                print("event: " .. tostring(event))
                
                                local practice = event:makeStage("icestom:race", {
                                    track = track,
                                    laps = 1,
                                    pits = 0
                                })
                
                                print("Practice: " .. tostring(practice))
                
                                local thread = practice:begin(results)
                
                                print("Thread: " .. tostring(thread))
                
                                local results = thread:join()
                
                                print("Results: " .. tostring(results))
                
                                return results
                            end
                
                            return {
                                name = "Race",
                            }, event
                            """)
                    );
                    luaEvent.begin(List.of(new Result<>(player))).whenComplete((results, throwable) -> {
                        log.info("results={}", results, throwable);
                        luaEvent.cleanup();
                    });
                } catch (LuauCompileException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
