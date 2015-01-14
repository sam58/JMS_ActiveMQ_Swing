package com.sam58;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.omg.PortableInterceptor.ACTIVE;


import javax.jms.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sam158 on 31.12.2014.
 * ДЕмка работы с JMS вообще и ActiveMQ в частности
 *
 */
public class JMSApplication
{
    private static JMSWindow jmsWindow; //Экземпляр рабочего окна
    private static ActiveMQConnectionFactory connectionFactory =null;//Фактория от apache ActiveMQ. Из нее получаю нужное
    private static Connection connection ;//коннект на MQ сервер
    private static Session session;// Сессия связи
    private Destination destination;//Буффер отправки-приемки сообщений
    private String queue;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
    
    public static Boolean connected(){
        try {
            if (connection == null) {
                connectionFactory = getConnectionFactory();
                connection=connectionFactory.createConnection();
                //получаем экзмпляр класса подключения
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                //создаем объект сессию без транзакций
                //параметром AUTO_ACKNOWLEDGE мы указали что отчет о доставке будет
                //отправляться автоматически при получении сообщения.



            } else {
                connection.start();
            }
            return true;
        }catch (JMSException ex){
            Logger.getLogger(JMSApplication.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private static ActiveMQConnectionFactory getConnectionFactory() {
        return new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,
                                                ActiveMQConnection.DEFAULT_PASSWORD,
                                                    "failover://tcp://localhost:61616");
    }

    /**
     * Constructor
     * @param jmsWindow Окна несоздает. Использует готовое
     */
    public JMSApplication(JMSWindow jmsWindow){
        this.jmsWindow= jmsWindow;
        this.jmsWindow.jBSendMessage.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        clickSendButton();
                    }
                }
        );
        this.jmsWindow.jBConnectedListener.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickConnectedButton();
            }
        });
        this.jmsWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(connection != null){
                    try {
                        connection.close();
                    } catch (JMSException ex) {
                        Logger.getLogger(JMSApplication.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    //если очереди или топика с таким название не существует на сервере JMS, то он будет создан автоматически.
    /**
     * Подключаемся к модели точка-точка.
     * @return
     */
    private Destination getDestinationQueue(){
        try {
            return session.createQueue(queue);
        } catch (JMSException ex) {
            Logger.getLogger(JMSApplication.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Подключаемся к модели подписчик/издатель.
     * @return
     */
    private Destination getDestinationTopic(){
        try {
            return session.createTopic(queue);
        } catch (JMSException ex) {
            Logger.getLogger(JMSApplication.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Реакция накнопку "отправить сообщение" c MQ
     */
    private void clickSendButton() {
        queue=this.jmsWindow.getQueueSendName();
        if(queue.equals(""))queue="simpleQueue";
        //Если связь успешна и есть что посылать
        if(connected() && !this.jmsWindow.getMessageSendText().equals("")){
            this.jmsWindow.sendConnectedSucces();//выставил строчку в окне
            if(this.jmsWindow.isPtP()){
               this.destination = this.getDestinationQueue();
            }else{
                this.destination = this.getDestinationTopic();
            }
            if(this.destination != null){
                try {
                    MessageProducer messageProducer=session.createProducer(destination);
                    messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);//парметром PERSISTENT указываем что сообщение
                    //будет хранится до тех пор пока не будет доставлено адресату.
                    //Создаем текстовое сообщение.
                    TextMessage message =session.createTextMessage(this.jmsWindow.getMessageSendText());
                    messageProducer.send(message);
                    this.jmsWindow.SendSuccess();
                } catch (JMSException e) {
                    this.jmsWindow.SendError();
                    e.printStackTrace();
                }
            }else{
                this.jmsWindow.SendError();
            }
        }else {this.jmsWindow.SendError();}
    }

    /**
     * реакция на кнопку посылки сообщения на MQ
     */
    private void clickConnectedButton() {
        queue=jmsWindow.getQueueSendName().equals("")?"SimpleQueue":jmsWindow.getQueueSendName();
        if (connected()){
            if (jmsWindow.isPtP()){
                destination=getDestinationQueue();
            }else{
                destination=getDestinationTopic();
            }
            if (destination!=null){
                try {
                    MessageConsumer consumer=session.createConsumer(destination);
                    consumer.setMessageListener(new MessageListener() {

                        @Override
                        public void onMessage(Message msg) {
                            TextMessage textmessage=(TextMessage)msg;
                            try {
                                jmsWindow.setTextReceiver(textmessage.getText());
                            } catch (JMSException ex) {
                                Logger.getLogger(JMSApplication.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    jmsWindow.ReceivedConnectedSucces();
                } catch (JMSException ex) {
                    Logger.getLogger(JMSApplication.class.getName()).log(Level.SEVERE, null, ex);
                    jmsWindow.ReceivedConnectedClose();
                }
            }else{jmsWindow.ReceivedConnectedClose();}
        }else{jmsWindow.ReceivedConnectedClose();}
    }


}
