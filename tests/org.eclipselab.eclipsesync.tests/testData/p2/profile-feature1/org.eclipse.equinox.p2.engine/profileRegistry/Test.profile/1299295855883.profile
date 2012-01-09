<?xml version='1.0' encoding='UTF-8'?>
<?profile version='1.0.0'?>
<profile id='Test' timestamp='1299295855883'>
  <properties size='7'>
    <property name='org.eclipse.equinox.p2.installFolder' value='D:\Indigo'/>
    <property name='org.eclipse.equinox.p2.cache' value='D:\Indigo'/>
    <property name='org.eclipse.update.install.features' value='true'/>
    <property name='org.eclipse.equinox.p2.roaming' value='true'/>
    <property name='org.eclipse.equinox.p2.environments' value='osgi.nl=en_US,osgi.ws=win32,osgi.arch=x86,osgi.os=win32'/>
    <property name='eclipse.touchpoint.launcherName' value='eclipse'/>
  </properties>
  <units size='2'>
    		<unit id='iu.feature1' version='1.0.0.I20110127-2034'>
			<update id='iu.feature1' range='0.0.0' severity='0' />
			<properties size='3'>
				<property name='org.eclipse.equinox.p2.name' value='Feature1' />
				<property name='lineUp' value='true' />
				<property name='org.eclipse.equinox.p2.type.group' value='true' />
			</properties>
			<provides size='1'>
				<provided namespace='org.eclipse.equinox.p2.iu' name='iu.feature1'
					version='1.0.0.I20110127-2034' />
			</provides>
			<requires size='13'>
				<required namespace='org.eclipse.equinox.p2.iu' name='iu.bundle1'
					range='[1.0.0,1.0.0]' />
			</requires>
			<touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0' />
			<unit id='iu.bundle1' version='1.0.0.I20110127-2034'
				singleton='false'>
				<update id='iu.bundle1'
					range='[0.0.0,1.0.0.I20110127-2034)' severity='0' />
				<properties size='7'>
					<property name='org.eclipse.equinox.p2.name' value='%featureName' />
					<property name='org.eclipse.equinox.p2.type.group' value='false' />
					<property name='df_LT.featureName' value='I am Bundle 1' />
				</properties>
				<provides size='2'>
					<provided namespace='org.eclipse.equinox.p2.iu'
						name='iu.bundle1' version='1.0.0.I20110127-2034' />
					<provided namespace='org.eclipse.equinox.p2.localization'
						name='df_LT' version='1.0.0' />
				</provides>
			</unit>
		</unit>
  </units>
  <iusProperties size='1'>
    <iuProperties id='iu.feature1' version='1.0.0.I20110127-2034'>
      <properties size='2'>
        <property name='org.eclipse.equinox.p2.internal.inclusion.rules' value='STRICT'/>
        <property name='org.eclipse.equinox.p2.type.root' value='true'/>
      </properties>
    </iuProperties>
  </iusProperties>
</profile>
