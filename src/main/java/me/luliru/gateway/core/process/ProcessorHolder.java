package me.luliru.gateway.core.process;

import lombok.Data;

/**
 * ProcessorHolder
 * Created by luliru on 2019/7/30.
 */
@Data
public class ProcessorHolder {

    private String name;

    private Processor processor;

    private ProcessorHolder next;

    private ProcessorHolder prev;
}
