# Notifry - Google App Engine backend
#
# Copyright 2011 Daniel Foote
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import web
from google.appengine.api import users
from lib.Renderer import Renderer
from lib.AC2DM import AC2DM
from model.UserDevices import UserDevices
import datetime

urls = (
	'/admin/', 'index',
	'/admin/stats/(.*)', 'stats',
	'/admin/users', 'users'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')
renderer.addTemplate('title', '')
renderer.addTemplate('user', users.get_current_user())

# Front page of Admin.
class index:
	def GET(self):
		return renderer.render('admin/index.html')

class stats:
	def GET(self, name):
		if name == '':
			# Stats index.
			return renderer.render('admin/stats/index.html')
		if name == 'counters':
			# Counters.
			summary = AC2DM.get_counter_summary()
			summary['buckets'].reverse()
			renderer.addData('counters', summary)
			return renderer.render('admin/stats/counters.html')

class users:
	def GET(self):
		devices = UserDevices.all()

		result = ""

		for device in devices:
			result += device.owner.email() + '\n'

		return result

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()