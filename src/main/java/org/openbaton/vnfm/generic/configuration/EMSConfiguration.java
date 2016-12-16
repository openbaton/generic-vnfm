/*
 *
 *  * Copyright (c) 2016 Fraunhofer FOKUS
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package org.openbaton.vnfm.generic.configuration;

import org.openbaton.vnfm.generic.interfaces.EmsInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Properties;

/** Created by lto on 09/11/15. */
@Configuration
@EnableRabbit
@ConfigurationProperties(prefix = "vnfm.rabbitmq")
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class EMSConfiguration {
  public static String queueName_emsRegistrator = "ems.generic.register";

  private RabbitAdmin rabbitAdmin;

  private boolean autodelete = true;
  private boolean durable;
  private boolean exclusive;
  private int minConcurrency;
  private int maxConcurrency;

  @Autowired(required = false)
  private EmsInterface registrator;

  @Autowired private ConnectionFactory connectionFactory;

  @Autowired(required = false)
  @Qualifier("listenerAdapter_emsRegistrator")
  private MessageListenerAdapter listenerAdapter_emsRegistrator;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  public void setMaxConcurrency(int maxConcurrency) {
    this.maxConcurrency = maxConcurrency;
  }

  public int getMinConcurrency() {
    return minConcurrency;
  }

  public void setMinConcurrency(int minConcurrency) {
    this.minConcurrency = minConcurrency;
  }

  public boolean isAutodelete() {
    return autodelete;
  }

  public void setAutodelete(boolean autodelete) {
    this.autodelete = autodelete;
  }

  public boolean isDurable() {
    return durable;
  }

  public void setDurable(boolean durable) {
    this.durable = durable;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }

  @PostConstruct
  private void init() {
    log.info("Initialization of RabbitConfiguration");
    rabbitAdmin = new RabbitAdmin(connectionFactory);
    TopicExchange topicExchange = new TopicExchange("openbaton-exchange");
    rabbitAdmin.declareExchange(topicExchange);
    log.info("exchange declared");
  }

  @Bean
  Queue queue_emsRegistrator() {
    Properties properties = new Properties();

    try {
      properties.load(ClassLoader.getSystemResourceAsStream("conf.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    queueName_emsRegistrator = "ems." + properties.getProperty("type") + ".register";

    return new Queue(queueName_emsRegistrator, durable, exclusive, autodelete);
  }

  @Bean
  TopicExchange exchange() {
    TopicExchange topicExchange = new TopicExchange("openbaton-exchange");
    return topicExchange;
  }

  @Bean
  Binding binding_vnfmCoreActionReply(TopicExchange exchange) {
    return BindingBuilder.bind(queue_emsRegistrator()).to(exchange).with(queueName_emsRegistrator);
  }

  @Bean
  MessageListenerAdapter listenerAdapter_emsRegistrator() {
    if (registrator != null) return new MessageListenerAdapter(registrator, "unregisterFromMsg");
    else return null;
  }

  @Bean
  SimpleMessageListenerContainer container_emsRegistrator(ConnectionFactory connectionFactory) {
    if (listenerAdapter_emsRegistrator != null) {
      SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
      container.setConnectionFactory(connectionFactory);
      container.setQueueNames(queueName_emsRegistrator);
      container.setConcurrentConsumers(1);
      container.setMaxConcurrentConsumers(15);
      container.setMessageListener(listenerAdapter_emsRegistrator);
      return container;
    } else return null;
  }
}
