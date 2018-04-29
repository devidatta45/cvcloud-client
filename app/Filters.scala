/**
  * Created by Donald Pollock on 10/05/2017.
  */

import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

class Filters @Inject()(corsFilter: CORSFilter)
  extends DefaultHttpFilters(corsFilter)
