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

import javax.annotation.PostConstruct;
import org.openbaton.common.vnfm_sdk.VnfmHelper;
import org.openbaton.vnfm.generic.interfaces.EmsInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@EnableRabbit
@ConfigurationProperties(prefix = "vnfm.rabbitmq")
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class EMSConfiguration {
  public static String queueName_emsRegistrator;

  private RabbitAdmin rabbitAdmin;

  ConnectionFactory emsConnectionFactory;

  private boolean durable;
  private boolean exclusive;
  private int minConcurrency;
  private int maxConcurrency;

  @Value("${vnfm.ems.start.timeout:500}")
  private int waitForEms;

  @Value("${vnfm.ems.queue.heartbeat}")
  protected String heartbeat;

  @Value("${vnfm.ems.queue.autodelete:true}")
  protected boolean autodelete;

  @Value("${vnfm.ems.version}")
  protected String version;

  @Value("${spring.rabbitmq.host}")
  private String brokerIp;

  @Value("${spring.rabbitmq.port}")
  private int rabbitPort;

  @Value("${vnfm.ems.username:admin}")
  private String emsRabbitUsername;

  @Value("${vnfm.ems.password:openbaton}")
  private String emsRabbitPassword;

  @Autowired(required = false)
  private EmsInterface registrator;

  @Autowired(required = false)
  @Qualifier("listenerAdapter_emsRegistrator")
  private MessageListenerAdapter listenerAdapter_emsRegistrator;

  private Logger log = LoggerFactory.getLogger(this.getClass());
  @Autowired private VnfmHelper vnfmHelper;

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

  public int getWaitForEms() {
    return waitForEms;
  }

  public void setWaitForEms(int waitForEms) {
    this.waitForEms = waitForEms;
  }

  public String getHeartbeat() {
    return heartbeat;
  }

  public void setHeartbeat(String heartbeat) {
    this.heartbeat = heartbeat;
  }

  public boolean isAutodelete() {
    return autodelete;
  }

  public void setAutodelete(boolean autodelete) {
    this.autodelete = autodelete;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @PostConstruct
  private void init() {
    log.info("Initialization of RabbitConfiguration");

    emsConnectionFactory = new CachingConnectionFactory();
    ((CachingConnectionFactory) emsConnectionFactory).setHost(brokerIp);
    ((CachingConnectionFactory) emsConnectionFactory).setPort(rabbitPort);
    ((CachingConnectionFactory) emsConnectionFactory).setUsername(emsRabbitUsername);
    ((CachingConnectionFactory) emsConnectionFactory).setPassword(emsRabbitPassword);

    rabbitAdmin = new RabbitAdmin(emsConnectionFactory);
    TopicExchange topicExchange = new TopicExchange("openbaton-exchange");
    rabbitAdmin.declareExchange(topicExchange);
    log.info("exchange declared");

    queueName_emsRegistrator = "ems." + vnfmHelper.getVnfmEndpoint() + ".register";
    rabbitAdmin.declareQueue(new Queue(queueName_emsRegistrator, durable, exclusive, autodelete));
  }

  @Bean
  MessageListenerAdapter listenerAdapter_emsRegistrator() {
    if (registrator != null) return new MessageListenerAdapter(registrator, "registerFromEms");
    else return null;
  }

  @Bean
  SimpleMessageListenerContainer container_emsRegistrator() {
    if (listenerAdapter_emsRegistrator != null) {
      SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
      container.setConnectionFactory(emsConnectionFactory);
      container.setQueueNames(queueName_emsRegistrator);
      container.setMessageListener(listenerAdapter_emsRegistrator);
      return container;
    } else return null;
  }

  public String getBrokerIp() {
    return brokerIp;
  }

  public int getRabbitPort() {
    return rabbitPort;
  }
}
