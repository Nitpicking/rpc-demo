package me.wuchong.rpc.framwork;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;

public class RpcFramwork {

	/**
	 * ��¶����
	 * 
	 * @param service
	 *            ����ʵ��
	 * @param port
	 *            ����˿�
	 */
	public static void export(final Object service, int port) {
		if (service == null)
			throw new IllegalArgumentException("service is null");
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException("Invalid port "+port);
		System.out.println("Export Service " + service.getClass().getName()
				+ " on port " + port);
		// ����socket����
		try {
			ServerSocket server = new ServerSocket(port);
			while (true) {
				final Socket socket = server.accept();
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							try {
								ObjectInputStream input = new ObjectInputStream(
										socket.getInputStream());

								String methodName = input.readUTF();
								Class<?>[] argTypes = (Class<?>[]) input
										.readObject();
								Object[] args = (Object[]) input.readObject();

								ObjectOutputStream output = new ObjectOutputStream(
										socket.getOutputStream());

								try {
									Method method = service.getClass()
											.getMethod(methodName, argTypes);
									Object result = method
											.invoke(service, args);
									output.writeObject(result);
								} catch (Throwable t) {
									output.writeObject(t);
								} finally {
									output.close();
									input.close();
								}

							} finally {
								socket.close();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ���÷���
	 * 
	 * @param <T>
	 *            �ӿڷ���
	 * @param interfaceClass
	 *            �ӿ�����
	 * @param host
	 *            ������������
	 * @param port
	 *            �������˿�
	 * @return Զ�̷���
	 */
	@SuppressWarnings("unchecked")
	public static <T> T refer(final Class<T> interfaceClass, final String host,
			final int port) {
		if (interfaceClass == null)
			throw new IllegalArgumentException("Interface class == null");
		if (!interfaceClass.isInterface())
			throw new IllegalArgumentException("The "
					+ interfaceClass.getName() + " must be interface class!");
		if (host == null || host.length() == 0)
			throw new IllegalArgumentException("Host == null!");
		if (port <= 0 || port > 65535)
			throw new IllegalArgumentException("Invalid port " + port);
		System.out.println("Invoke remote service " + interfaceClass.getName()
				+ " from server " + host + ":" + port);

		T proxy = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),new Class<?>[] {interfaceClass}, 
				new InvocationHandler(){

					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						
						Socket socket = new Socket(host, port);
						ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						
						try{
						output.writeUTF(method.getName());
						output.writeObject(method.getParameterTypes());
						output.writeObject(args);
						
						
						ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
						try{
							Object result = input.readObject();
							if(result instanceof Throwable){
								throw (Throwable)result;
							}
							return result;
						}finally{
							input.close();
						}
						
						} finally {  
	                        output.close();  
	                        socket.close();
	                    }  
					}});
		return proxy;

	}
}
